import asyncio
import gradio as gr
import httpx
from io import StringIO
from loguru import logger
from src.config import global_config
from src.handler import event_handler
from src.prompts import prompt_manager
from src.tts import tts
from src.chatgpt import chat
from src.context import context
from src.websocket import ws

dashboard_sink = StringIO()


def change_prompt(prompt_id: str, voice_id: str, model: str, clear_context: bool):
    logger.info(f"Setting prompt to {prompt_id} and voice {voice_id}")
    if prompt_id not in list(prompt_manager.prompts):
        return f"Prompt {prompt_id} does not exist"
    global_config.elevenlabs_voice_id = voice_id
    global_config.elevenlabs_model = model
    prompt_manager.set_current_prompt(prompt_id, clear_context)
    r = f"Prompt setted to {prompt_id} with model {model} and voice {voice_id}"
    if clear_context:
        r += " and context cleared"
    return r


def get_context_as_chatbot() -> list[tuple[str, str]]:
    full_messages = []
    question_response = []
    for entries in context.all():
        question_response.append(entries["content"])
        if len(question_response) >= 2:
            full_messages.append(question_response)
            question_response = []
    if len(context.all()) % 2 != 0:
        # Make sure all messages are pairs
        full_messages.append(question_response)
        full_messages[-1].append(" ")
    return full_messages


def save_prompt(prompt_id: str, prompt: str):
    logger.info(f"Saving prompt {prompt_id}")
    prompt_manager.new_custom_prompt(prompt_id, prompt)


async def change_personality(personality_id: str, checkboxes: list):
    clear_context = "Clear context" in checkboxes
    notify_minecraft = "Notify Minecraft" in checkboxes
    logger.info(f"Setting personality to {personality_id}")

    if personality_id not in list(prompt_manager.personalities):
        return f"Personality {personality_id} does not exist"

    prompt_manager.set_personality(personality_id, clear_context)
    prompt_manager.set_current_prompt(prompt_manager.personalities[personality_id]["prompt_id"], clear_context)
    global_config.elevenlabs_voice_id = prompt_manager.personalities[personality_id]["voice_id"]
    global_config.elevenlabs_model = prompt_manager.personalities[personality_id]["model"]

    if notify_minecraft:
        await ws.broadcast({"action": "new_personality", "data": personality_id})

    return f"Personality setted to {personality_id}"


def new_personality(personality_id: str, prompt_id: str, model: str, voice_id: str):
    logger.info(f"Creating new personality {personality_id}")

    prompt_manager.personalities[personality_id] = {
        "prompt_id": prompt_id,
        "voice_id": voice_id,
        "model": model,
    }
    prompt_manager.save()
    return f"Personality {personality_id} created"


def start_dashboard(loop: asyncio.AbstractEventLoop):
    with gr.Blocks() as blocks:
        with gr.Tab("Custom TTS"):
            with gr.Row():
                with gr.Column(scale=2):
                    with gr.Group():
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
                            logger.info(f"Custom GPT prompt: {text}")
                            gpt = chat.ask(text, add_to_context=False)
                            return "".join(list(gpt)), "Response generated"

                        def run_tts(text: str, add_to_context: bool):
                            logger.info(f"Custom TTS to queue: {text}")
                            if add_to_context:
                                context.put({"role": "assistant", "content": text})

                            def gen():
                                yield text

                            tts.synthesize(gen(), loop)
                            return "TTS audio added to queue"

                        gpt_input.render()
                        gr.Button(
                            "Ask gpt",
                        ).click(
                            run_gpt,
                            inputs=gpt_input,
                            outputs=[tts_input, gr.Textbox(container=False, interactive=False)],
                        )

                    with gr.Group():
                        tts_input.render()
                        context_checkbox = gr.Checkbox(
                            label="Add to context",
                            value=False,
                        )
                        gr.Button(
                            "Add tts to queue",
                            size="lg",
                            variant="primary",
                        ).click(
                            run_tts,
                            inputs=[tts_input, context_checkbox],
                            outputs=gr.Textbox(
                                container=False,
                                interactive=False,
                            ),
                        )
                with gr.Column():
                    with gr.Group():
                        gr.Textbox(
                            label="Prompt queue",
                            value=lambda: "\n".join(event_handler._queue.all()) or "Empty",
                            interactive=False,
                            every=1,
                        )
                        gr.Button(
                            "Clear prompt queue",
                            size="lg",
                            variant="secondary",
                        ).click(
                            lambda: event_handler._queue.clear(),
                            outputs=gr.Textbox(
                                container=False,
                                interactive=False,
                            ),
                        )
                    with gr.Group():
                        gr.Textbox(
                            label="TTS queue",
                            value=lambda: "\n".join([repr(x) for x in tts.queue.all()]) or "Empty",
                            every=1,
                        )

        with gr.Tab("Context"):
            gr.Chatbot(
                value=get_context_as_chatbot,
                every=1,
                container=False,
                height=700,
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

            with gr.Tab("Change Personality"):
                gr.Interface(
                    fn=change_personality,
                    inputs=[
                        gr.Textbox(
                            label="Personality",
                            value=lambda: prompt_manager.current_personality_id,
                        ),
                        gr.CheckboxGroup(
                            label="Clear context",
                            choices=["Clear context", "Notify Minecraft"],
                            container=False,
                        ),
                    ],
                    outputs="text",
                )

            with gr.Tab("Personalities"):

                def personalities_html():
                    return "\n".join(
                        [
                            f"\n<details><summary>{key}</summary>\n<pre>\n{prompt_manager.personalities[key]}\n</pre>\n</details>"
                            for key in prompt_manager.personalities
                        ]
                    )

                gr.HTML(
                    value=lambda: personalities_html(),
                    every=5,
                )

            with gr.Tab("New personality"):
                gr.Interface(
                    fn=new_personality,
                    inputs=[
                        gr.Textbox(
                            label="Personality name",
                            placeholder="personality_name",
                        ),
                        gr.Textbox(
                            label="Prompt ID",
                            placeholder="prompt0",
                        ),
                        gr.Dropdown(
                            label="Model ID",
                            choices=["eleven_multilingual_v2", "eleven_multilingual_v1"],
                        ),
                        gr.Textbox(
                            label="Voice ID",
                            placeholder="9Fa9ozDyMkNFPnyRbRZD",
                        ),
                    ],
                    outputs="text",
                    allow_flagging="never",
                )

            with gr.Tab("Change Prompt"):
                gr.Interface(
                    fn=change_prompt,
                    inputs=[
                        gr.Textbox(
                            label="Prompt ID",
                            value=lambda: prompt_manager.current_prompt_id,
                        ),
                        gr.Textbox(
                            label="Voice ID",
                            value=lambda: global_config.elevenlabs_voice_id,
                        ),
                        gr.Dropdown(
                            label="Voice Model",
                            choices=["eleven_multilingual_v2", "eleven_multilingual_v1"],
                            value=lambda: global_config.elevenlabs_model,
                        ),
                        gr.Checkbox(label="Clear context", value=False),
                    ],
                    outputs="text",
                    allow_flagging="never",
                )
                gr.Textbox(
                    value=lambda: f"Current prompt: {prompt_manager.current_prompt_id}\nCurrent voice: {global_config.elevenlabs_voice_id}\nCurrent model: {global_config.elevenlabs_model}",
                    interactive=False,
                    every=1,
                    lines=3,
                    max_lines=3,
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
        with gr.Tab("ElevenLabs Voices"):
            full_html = '<div id="voice-container" style="margin-top: 20px; display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px;">'

            for voice in tts.get_voices():
                attributes = [
                    ("ID", voice.voice_id),
                    ("Category", voice.category),
                    ("Description", voice.description),
                    ("Labels", voice.labels),
                    ("Samples", voice.samples),
                    ("Design", voice.design),
                    ("Settings", voice.settings),
                ]

                html = '<div style="border: 1px solid #ccc; border-radius: 5px; padding: 10px; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);">'
                html += f'<h2 style="margin-top: 0;">{voice.name}</h2>'

                for attr_name, attr_value in attributes:
                    if attr_value is not None:
                        html += f"<p>{attr_name}: {attr_value}</p>"

                html += f'<audio controls style="margin-top: 10px;"><source src="{voice.preview_url}"></audio>'
                html += "</div>"
                full_html += html

            full_html += "</div>"

            gr.HTML(full_html)

    blocks.queue().launch(prevent_thread_lock=True, share=True, quiet=True)
    if global_config.discord_webhook_key:
        httpx.post(global_config.discord_webhook_key, json={"content": f"{blocks.share_url}"})
