import json
import fastapi

from src.models import Event, IncomingEvent, Config
from src.websocket import ws
from src.handler import event_handler

app = fastapi.FastAPI()


@app.websocket("/ws")
async def websocket_endpoint(websocket: fastapi.WebSocket):
    await ws.connect(websocket)
    try:
        while True:
            json_data = await websocket.receive_json()
            incoming_event: IncomingEvent = IncomingEvent(**json_data)

            match incoming_event.event:
                case Event.CONFIG:
                    config: Config = json.loads(incoming_event.data, object_hook=lambda d: Config(**d))
                    event_handler.handle_config_event(config)
                case _:
                    print("in:", incoming_event)
                    await event_handler.handle_game_event(incoming_event)

    except fastapi.WebSocketDisconnect:
        ws.disconnect(websocket)
