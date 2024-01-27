import asyncio
import random
import threading
from loguru import logger

from src.chatgpt import chat
from src.config import global_config
from src.cooldown import CooldownManager
from src.models import Action, Config, IncomingEvent, OutgoingAction
from src.queue import Queue
from src.tts import tts
from src.websocket import ws


class EventHandler:
    def __init__(self):
        self._cd_manager = CooldownManager()
        self._queue = Queue(maxsize=8, join_duplicates=True)

    def handle_cooldowns_and_queue(self, event: IncomingEvent) -> OutgoingAction:
        self._queue.put(event.data)

        if self._cd_manager.check_all_cooldown(event.event):
            logger.info("Ignoring event due to cooldown")
            return OutgoingAction(
                action=Action.IGNORE,
                data="Aguardando cooldown",
            )

        outgoing_action = OutgoingAction(
            action=Action.SEND_CHAT,
            data="\n".join(self._queue.all()),
        )
        logger.debug(f"Queue: {self._queue.all()!r}")
        self._queue.clear()

        self._cd_manager.add_cooldown(
            event.event,
            global_config.cooldown_individual * 60,
        )  # Individual cd, 5 min

        self._cd_manager.add_cooldown(
            "GLOBAL_COOLDOWN", global_config.cooldown_global + random.randint(0, 30)
        )  # Global cd, 30 sec to 1 min

        return outgoing_action

    async def handle_game_event(self, event: IncomingEvent) -> None:
        outgoing = self.handle_cooldowns_and_queue(event)

        if outgoing.action == Action.IGNORE:
            await ws.broadcast(outgoing.model_dump())
            return

        gpt_response_generator = chat.ask(outgoing.data)

        threading.Thread(
            target=tts.synthesize,
            kwargs={
                "gen": gpt_response_generator,
                "loop": asyncio.get_event_loop(),
            },
        ).start()

    def handle_config_event(self, req_config: Config):
        logger.info("Updating configs received from client")
        global_config.set_all(req_config)
        chat.set_config(global_config)
        tts.set_config(global_config)
        global_config.save()


event_handler = EventHandler()
