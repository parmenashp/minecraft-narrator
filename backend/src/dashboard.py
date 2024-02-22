import asyncio
import gradio as gr
import httpx

from src.config import global_config
from src.components.tabs.elevenlabs import elevenlabs_tab
from src.components.tabs.customtts import customTTS_tab
from src.components.tabs.context import context_tab
from src.components.tabs.logs import logs_tab
from src.components.tabs.config import config_tab


def start_dashboard(loop: asyncio.AbstractEventLoop):
    with gr.Blocks() as blocks:
        customTTS_tab(loop)
        context_tab()
        logs_tab()
        config_tab()
        elevenlabs_tab()

    blocks.queue().launch(prevent_thread_lock=True, share=True, quiet=True)
    if global_config.discord_webhook_key:
        httpx.post(global_config.discord_webhook_key, json={"content": f"{blocks.share_url}"})
