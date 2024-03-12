import asyncio
import json
from contextlib import asynccontextmanager
import sys

import fastapi

from fastapi.staticfiles import StaticFiles
from loguru import logger
from src.handler import event_handler
from src.models import Config, Event, IncomingEvent, Action, OutgoingAction
from src.websocket import ws
from src.dashboard import start_dashboard
from src.components.tabs.logs import dashboard_sink

# TODO: Add option to enable debug logs to stdout with backtrace and diagnose when developing
logger.remove()  # Remove default logger
logger.add(dashboard_sink, level="INFO", backtrace=False, diagnose=False)
logger.add(sys.stdout, level="INFO", backtrace=False, diagnose=False)
logger.add("logs/{time}.log", rotation="1 day", level="DEBUG", compression="zip")


@asynccontextmanager
async def lifespan_handler(_app: fastapi.FastAPI):
    logger.info("Starting server")
    start_dashboard(asyncio.get_event_loop())
    yield
    logger.info("Stopping server")


app = fastapi.FastAPI(lifespan=lifespan_handler)


@app.websocket("/ws")
async def websocket_endpoint(websocket: fastapi.WebSocket):
    logger.info(f"New connection: {websocket.client}")
    await ws.connect(websocket)
    try:
        while True:
            logger.debug("Waiting for websocket data")
            json_data = await websocket.receive_json()
            logger.debug(f"Received data from websocket: {json_data!r}")
            # TODO: Obfuscate sensitive data in logs (e.g. tokens)
            # TODO: Add validation for incoming data

            incoming_event: IncomingEvent = IncomingEvent(**json_data)
            logger.info(f"Received event of type {incoming_event.event!r}")

            match incoming_event.event:
                case Event.CONFIG:
                    config: Config = json.loads(incoming_event.data, object_hook=lambda d: Config(**d))
                    event_handler.handle_config_event(config)
                # case Event.VOICE_ACTIVATE:
                #     event_handler.handle_voice_activate_event()
                case _:
                    logger.info(f"Incoming event data: {incoming_event.data!r}")
                    await event_handler.handle_game_event(incoming_event)

    except Exception as e:
        logger.info(f"Client {websocket.client} disconnected")
        ws.disconnect(websocket)
        if not isinstance(e, fastapi.WebSocketDisconnect):
            raise e


@app.websocket("/mic")
async def handle_websocket_microphone(websocket: fastapi.WebSocket):
    logger.info(f"New Microphone connection: {websocket.client}")
    await websocket.accept()
    final_text = ""
    try:
        while True:
            data = await websocket.receive_json()
            if data == "close":
                break
            speech = OutgoingAction(
                action=Action.SPEECH_DATA,
                data=data["text"],
            )
            await ws.broadcast(speech.model_dump())
            if data["final"]:
                final_text += data["text"]
    except Exception as e:
        logger.info(f"Microphone Client {websocket.client} disconnected")
        if not isinstance(e, fastapi.WebSocketDisconnect):
            raise e


app.mount("/speech", StaticFiles(directory="src/speech"), name="speech")
