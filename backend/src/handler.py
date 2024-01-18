import random
from src.cooldown import CooldownManager
from src.models import IncomingEvent, OutgoingAction, Action
from src.queue import Queue
from src.config import global_config


class EventHandler:
    def __init__(self):
        self._cd_manager = CooldownManager()
        self._queue = Queue()

    def handle(self, event: IncomingEvent) -> OutgoingAction:
        self._queue.put(event.data)

        outgoing_action = OutgoingAction(
            action=Action.SEND_CHAT,
            data={"text": ""},
        )

        if self._cd_manager.check_all_cooldown(event.event):
            return OutgoingAction(
                action=Action.IGNORE,
                data={"text": "Aguardando cooldown"},
            )

        outgoing_action.data["text"] = "\n".join(self._queue.all())
        print(self._queue.all())
        self._queue.clear()

        self._cd_manager.add_cooldown(
            event.event,
            global_config.cooldown_individual * 60,
        )  # Individual cd, 5 min

        self._cd_manager.add_cooldown(
            "GLOBAL_COOLDOWN", global_config.cooldown_global + random.randint(0, 30)
        )  # Global cd, 30 sec to 1 min

        return outgoing_action


event_handler = EventHandler()
