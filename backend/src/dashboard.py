import asyncio
import gradio as gr
import httpx
from fastapi.staticfiles import StaticFiles
from loguru import logger
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

    blocks.app.mount("/speech", StaticFiles(directory="src/speech"), name="speech")

    if global_config.discord_webhook_key:
        httpx.post(global_config.discord_webhook_key, json={"content": f"{blocks.share_url}"})

    async def handle_websocket_microphone(websocket):
        await websocket.accept()
        while True:
            data = await websocket.receive_json()
            if data == "close":
                break
            if data["final"]:
                logger.info(data)

    blocks.app.add_websocket_route("/micws", handle_websocket_microphone)
