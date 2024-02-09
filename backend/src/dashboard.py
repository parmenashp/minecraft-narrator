import asyncio
import gradio as gr
import httpx
from io import StringIO
from loguru import logger
from src.config import global_config
from src.prompts import prompt_manager
from src.tts import tts
from src.chatgpt import chat

prompt_ids = list(prompt_manager.prompts)
dashboard_sink = StringIO()


def start_dashboard(loop: asyncio.AbstractEventLoop):
    def change_prompt(prompt_id: str, clear_context: bool):
        logger.info(f"Setting prompt to {prompt_id}")
        prompt_manager.set_current_prompt(prompt_id, clear_context)
        r = "Prompt setted to " + prompt_id
        if clear_context:
            r += " and context cleared"
        return r

    with gr.Blocks() as blocks:
        with gr.Tab("Configs"):
            gr.Interface(
                fn=change_prompt,
                inputs=[
                    gr.Dropdown(
                        list(prompt_ids),
                        label="Prompts",
                        value=lambda: prompt_manager.current_prompt_id,
                    ),
                    gr.Checkbox(label="Clear context", value=False),
                ],
                outputs="text",
                title="Change Prompt",
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
            for id, prompt in prompt_manager.prompts.items():
                with gr.Tab(id):
                    gr.Markdown(prompt)

        with gr.Tab("Custom TTS"):

            def run_gpt(text):
                logger.info(f"Custom TTS: {text}")
                gpt = chat.ask(text)
                return "".join(list(gpt))

            def run_tts(text: str):
                def gen():
                    yield text

                tts.synthesize(gen(), loop)
                return "TTS audio added to queue"

            gr.Interface(
                fn=run_gpt,
                inputs=[
                    gr.Textbox(
                        label="Ask gpt with current prompt",
                        placeholder="Jogador Feeeelps morreu",
                    )
                ],
                outputs="text",
                allow_flagging="never",
            )
            gr.Interface(
                fn=run_tts,
                inputs=[
                    gr.Textbox(
                        label="Add tts text to queue",
                        placeholder="Ah, que pena felps morreu.",
                    ),
                ],
                outputs="text",
                allow_flagging="never",
            )

        with gr.Tab("Logs"):
            gr.Code(
                value=lambda: dashboard_sink.getvalue(),  # type: ignore
                label="Logs",
                interactive=False,
                every=1,
                language="typescript",
            )

    blocks.queue().launch(prevent_thread_lock=True, share=True, quiet=True)
    if global_config.discord_webhook_key:
        httpx.post(global_config.discord_webhook_key, json={"content": f"{blocks.share_url}"})
