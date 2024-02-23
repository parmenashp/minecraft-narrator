from typing import Iterator
import gradio as gr
from loguru import logger
from src.chatgpt import chat
from src.context import context
from src.tts import tts
from src.handler import event_handler


def customTTS_tab(loop):
    with gr.Tab("Custom TTS") as tab:
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

                    def run_gpt(text: str) -> tuple[str, str]:
                        logger.info(f"Custom GPT prompt: {text}")
                        gpt = chat.ask(text, add_to_context=False)
                        gpt = gpt()

                        if isinstance(gpt, str):
                            return gpt, "Response generated"
                        if isinstance(gpt, Iterator):
                            return "".join(list(gpt)), "Response generated"

                        return "No response", "No response"

                    def run_tts(text: str, add_to_context: bool):
                        logger.info(f"Custom TTS to queue: {text}")
                        if add_to_context:
                            context.put({"role": "assistant", "content": text})

                        tts.synthesize(text, loop)
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
    return tab
