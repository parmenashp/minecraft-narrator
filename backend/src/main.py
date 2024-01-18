import threading
import fastapi

from src.models import Action, OutgoingAction, IncomingEvent, Pong, Config
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
            incoming: IncomingEvent = IncomingEvent(**json_data)

            print("in:", incoming)

            outgoing: OutgoingAction = event_handler.handle(event=incoming)

            if outgoing.action == Action.IGNORE:
                await ws.broadcast(outgoing.model_dump())
                continue

            gpt_response_generator = chat.ask(outgoing.data)
            threading.Thread(target=tts.synthesize, kwargs={"gen": gpt_response_generator}).start()
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
