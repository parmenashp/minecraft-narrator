import asyncio
import threading
import fastapi

from src.models import Action, OutgoingAction, IncomingEvent, Pong, Config
from src.handler import event_handler
from src.chatgpt import chat
from src.config import global_config
from src.tts import tts

app = fastapi.FastAPI()


@app.post("/event")
def handle_event(event: IncomingEvent) -> OutgoingAction:
    print("in:", event)
    r = event_handler.handle(event)
    if r.action == Action.IGNORE:
        return r

    text = r.data["text"]
    chat_response = chat.ask(text)
    text = tts.synthesize(chat_response)
    print("chat_response:", text)
    if text == "":
        r.action = Action.IGNORE
        text = "Erro ao gerar texto"
    r.data["text"] = text
    print("out:", r)
    return r


@app.websocket("/ws")
async def websocket_endpoint(websocket: fastapi.WebSocket):
    await websocket.accept()
    while True:
        try:
            json_data = await websocket.receive_json()
            incoming: IncomingEvent = IncomingEvent(**json_data)

            print("in:", incoming)

            outgoing = event_handler.handle(event=incoming)
            if outgoing.action == Action.IGNORE:
                await websocket.send_json(outgoing.model_dump())
                continue

            gpt_prompt = outgoing.data["text"]

            def background(loop):
                gpt_response_generator = chat.ask(gpt_prompt)
                full_response = tts.synthesize(gpt_response_generator)

                if full_response == "":
                    response = OutgoingAction(
                        action=Action.IGNORE,
                        data={"text": "Erro ao gerar texto"},
                    )
                else:
                    response = OutgoingAction(
                        action=Action.SEND_CHAT,
                        data={"text": full_response},
                    )
                asyncio.run_coroutine_threadsafe(websocket.send_json(response.model_dump()), loop)

            loop = asyncio.get_event_loop()
            threading.Thread(target=background, kwargs={"loop": loop}).start()

            print("received:", incoming)
        except Exception as e:
            if websocket.client_state == fastapi.websockets.WebSocketState.CONNECTED:
                response = {"error": str(e)}
                await websocket.send_json(response)
            else:
                raise e


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
