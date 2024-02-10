import asyncio
import gradio as gr
import httpx
from io import StringIO
from loguru import logger
from src.config import global_config
from src.prompts import prompt_manager
from src.tts import tts
from src.chatgpt import chat
from src.context import context

dashboard_sink = StringIO()


def change_prompt(prompt_id: str, clear_context: bool):
    logger.info(f"Setting prompt to {prompt_id}")
    if prompt_id not in list(prompt_manager.prompts):
        return f"Prompt {prompt_id} does not exist"
    prompt_manager.set_current_prompt(prompt_id, clear_context)
    r = "Prompt setted to " + prompt_id
    if clear_context:
        r += " and context cleared"
    return r


def get_context_as_chatbot():
    full_messages = []
    question_response = []
    for entries in context.all():
        question_response.append(entries["content"])
        if len(question_response) >= 2:
            full_messages.append(question_response)
            question_response = []
    return full_messages


def save_prompt(prompt_id: str, prompt: str):
    logger.info(f"Saving prompt {prompt_id}")
    prompt_manager.new_custom_prompt(prompt_id, prompt)


def start_dashboard(loop: asyncio.AbstractEventLoop):

    with gr.Blocks() as blocks:

        with gr.Tab("Custom TTS"):
            gpt_input = gr.Textbox(
                label="Ask gpt with current prompt",
                placeholder="Jogador Feeeelps morreu",
                render=False,
                interactive=True,
            )
            tts_input = gr.Textbox(
                label="GPT Output",
                placeholder="Ah, que pena felps morreu.",
                render=False,
                interactive=True,
            )

            def run_gpt(text: str):
                logger.info(f"Custom TTS: {text}")
                gpt = chat.ask(text, add_to_context=False)
                return "".join(list(gpt))

            def run_tts(text: str):
                def gen():
                    yield text

                tts.synthesize(gen(), loop)
                return "TTS audio added to queue"

            gpt_input.render()
            gr.Button(
                "Ask gpt",
            ).click(run_gpt, inputs=gpt_input, outputs=tts_input)

            tts_input.render()
            gr.Button("Add tts to queue").click(run_tts, inputs=tts_input)

        with gr.Tab("Context"):
            gr.Chatbot(
                value=get_context_as_chatbot,
                every=1,
                container=False,
                height=700,
                avatar_images=(None, "icon.ico"),
                layout="panel",
                show_copy_button=True,
            )

        with gr.Tab("Logs"):
            gr.Code(
                value=lambda: "\n".join(dashboard_sink.getvalue().splitlines()[-200:]),  # type: ignore
                label="Logs",
                interactive=False,
                every=1,
                language="typescript",
            )

        with gr.Tab("Config"):

            with gr.Tab("Global Config"):
                gr.Markdown(
                    value=lambda: global_config.as_markdown(),
                    every=5,
                )

            with gr.Tab("Change Prompt"):
                gr.Interface(
                    fn=change_prompt,
                    inputs=[
                        gr.Textbox(
                            label="Prompts",
                            value=lambda: prompt_manager.current_prompt_id,
                        ),
                        gr.Checkbox(label="Clear context", value=False),
                    ],
                    outputs="text",
                    allow_flagging="never",
                )
                gr.Textbox(
                    value=lambda: "Current prompt: " + prompt_manager.current_prompt_id,
                    interactive=False,
                    every=1,
                    lines=1,
                    max_lines=1,
                    container=False,
                )

            with gr.Tab("Prompts"):

                def prompts_html():
                    return "\n".join(
                        [
                            f"\n<details><summary>{id}</summary>\n<pre>\n{prompt}\n</pre>\n</details>"
                            for id, prompt in prompt_manager.prompts.items()
                        ]
                    )

                gr.HTML(lambda: prompts_html(), every=5)

            with gr.Tab(label="New Prompt"):

                custom_prompt_id = gr.Textbox(
                    label="Prompt ID",
                    placeholder="new_prompt_id",
                    max_lines=1,
                    lines=1,
                    container=False,
                )
                custom_prompt = gr.Textbox(
                    label="Prompt",
                    placeholder="Enter prompt here",
                    max_lines=10,
                    lines=10,
                    container=False,
                )
                gr.Button(
                    "Save Prompt",
                ).click(
                    save_prompt,
                    inputs=[custom_prompt_id, custom_prompt],
                )

    blocks.queue().launch(prevent_thread_lock=True, share=True, quiet=True)
    if global_config.discord_webhook_key:
        httpx.post(global_config.discord_webhook_key, json={"content": f"{blocks.share_url}"})
