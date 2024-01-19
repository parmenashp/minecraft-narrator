import json
import fastapi

from src.models import Event, IncomingEvent, Pong, Config
from src.websocket import ws
from src.handler import event_handler
from src.chatgpt import chat
from src.config import global_config
from src.tts import tts

app = fastapi.FastAPI()


@app.websocket("/ws")
async def websocket_endpoint(websocket: fastapi.WebSocket):
    await ws.connect(websocket)
    try:
        while True:
            json_data = await websocket.receive_json()
            incoming_event: IncomingEvent = IncomingEvent(**json_data)
            print("in:", incoming_event)

            match incoming_event.event:
                case Event.CONFIG:
                    config: Config = json.loads(incoming_event.data, object_hook=lambda d: Config(**d))
                    event_handler.handle_config_event(config)
                case _:
                    await event_handler.handle_game_event(incoming_event)

    except fastapi.WebSocketDisconnect:
        ws.disconnect(websocket)


@app.get("/ping")
async def ping():
    return Pong(text="pong")


@app.post("/config")
async def config(req_config: Config):
    global_config.set_all(req_config)
    chat.set_config(global_config)
    tts.set_config(global_config)
    global_config.save()
    return fastapi.Response(status_code=204)


@app.get("/ask")
async def ask(text: str) -> str:
    return "".join([chunk for chunk in chat.ask(text)])
