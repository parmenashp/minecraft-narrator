import json
from contextlib import asynccontextmanager
import sys

import fastapi
from loguru import logger

from src.handler import event_handler
from src.models import Config, Event, IncomingEvent
from src.websocket import ws


# TODO: Add option to enable debug logs to stdout with backtrace and diagnose when developing
logger.remove()  # Remove default logger
logger.add(sys.stdout, level="INFO", backtrace=False, diagnose=False)
logger.add("logs/{time}.log", rotation="1 day", level="DEBUG", compression="zip")


@asynccontextmanager
async def lifespan_handler(_app: fastapi.FastAPI):
    logger.info("Starting server")
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
                case _:
                    logger.info(f"Incoming event data: {incoming_event.data!r}")
                    await event_handler.handle_game_event(incoming_event)

    except fastapi.WebSocketDisconnect:
        logger.info(f"Client {websocket.client} disconnected")
        ws.disconnect(websocket)
