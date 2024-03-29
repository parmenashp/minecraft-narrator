from elevenlabs import Voice
import gradio as gr
from src.tts import tts
from src.config import global_config
from loguru import logger


def clone_voice(name: str, files: list[str]):
    logger.info(f"Cloning voice {name}")
    msg, voice = tts.clone_voice_from_files(name, files)
    if voice:
        return f"New Voice {name} created ID: {voice.voice_id}"
    else:
        return f"Error: {msg}"


def gen_voices_html():
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
    return gr.HTML(full_html)


def delete_voice(voice_id: str) -> str:
    voice = Voice(voice_id=voice_id)
    try:
        voice.delete()
        logger.info(f"Voice {voice_id} deleted")
        return "Voice deleted with success!"
    except Exception as e:
        logger.info(f"Failed to delete {voice_id} error: {e}")
        return f"Failed to delete voice error: {e}"


def set_voice_settings(stability: float, style: float, similarity_boost: float):
    global_config.voice_stability = stability
    global_config.voice_style = style
    global_config.voice_similarity_boost = similarity_boost
    global_config.save()
    return "Voice settings updated with success!"


def elevenlabs_tab():
    with gr.Tab("Elevenlabs") as tab:
        with gr.Tab("Available Voices"):
            gr.Button("Refresh Voices").click(
                gen_voices_html,
                outputs=gr.HTML(),
            )

        with gr.Tab("Instant Voice Clone"):
            gr.Interface(
                fn=clone_voice,
                inputs=[
                    gr.Textbox(
                        label="Voice name",
                        placeholder="Voz legal 1",
                    ),
                    gr.File(
                        label="Audio files (max 25)",
                        file_count="multiple",
                        file_types=["audio"],
                        type="filepath",
                    ),
                ],
                outputs="text",
                allow_flagging="never",
            )

        with gr.Tab("Delete Voice Clone"):
            gr.Interface(
                fn=delete_voice,
                inputs=[gr.Textbox(label="Voice ID to delete")],
                outputs="text",
                allow_flagging="never",
            )

        with gr.Tab("Voice Settings"):
            gr.Interface(
                fn=set_voice_settings,
                inputs=[
                    gr.Slider(
                        label="Stability",
                        value=global_config.voice_stability,
                        minimum=0.01,
                        maximum=1.00,
                        step=0.01,
                    ),
                    gr.Slider(
                        label="Style",
                        value=global_config.voice_style,
                        minimum=0.01,
                        maximum=1.00,
                        step=0.01,
                    ),
                    gr.Slider(
                        label="Similarity Boost",
                        value=global_config.voice_similarity_boost,
                        minimum=0.01,
                        maximum=1.00,
                        step=0.01,
                    ),
                ],
                outputs="text",
                allow_flagging="never",
            )

    return tab
