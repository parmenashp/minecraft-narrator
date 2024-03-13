from loguru import logger
import json
import fastapi
from src.websocket import ws
from src.models import IncomingEvent, Action, OutgoingAction
from src.handler import event_handler

class PlayerVoice:
    def __init__(self):
        self.voice_listening = False
        self.mic_ws: fastapi.WebSocket | None = None

    async def handle_voice_activate(self):
        if self.mic_ws is not None:
            await self.mic_ws.send_text("start_listening")
        self.voice_listening = True

    async def handle_voice_complete(self, incoming_event: IncomingEvent):
        if self.mic_ws is not None:
            await self.mic_ws.send_text("stop_listening")
        self.voice_listening = False
        logger.info(f"Incoming voice data: {incoming_event.data!r}")
        await event_handler.handle_game_event(incoming_event)

    async def handle_websocket_microphone(self, websocket: fastapi.WebSocket):
        logger.info(f"New Microphone connection: {websocket.client}")
        await websocket.accept()
        self.mic_ws = websocket
        try:
            while True:
                data = await websocket.receive_json()
                if data == "close":
                    break
                speech = OutgoingAction(
                    action=Action.SPEECH_DATA,
                    data=json.dumps(data, ensure_ascii=False),
                )
                if self.voice_listening:
                    await ws.broadcast(speech.model_dump())
        except Exception as e:
            logger.info(f"Microphone Client {websocket.client} disconnected")
            self.mic_ws = None
            if not isinstance(e, fastapi.WebSocketDisconnect):
                raise e

voice = PlayerVoice()
