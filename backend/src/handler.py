import random
from fastapi import HTTPException
from src.cooldown import CooldownManager
from src.models import IncomingEvent, Event, OutgoingAction, Action
from typing import Callable, TypeVar, Awaitable

T = TypeVar("T", bound=IncomingEvent)


class EventHandler:
    def __init__(self):
        self._handlers = {}
        self._cd_manager = CooldownManager()

    def register(
        self, event: Event
    ) -> Callable[[Callable[[T], Awaitable[OutgoingAction]]], Callable[[T], Awaitable[OutgoingAction]]]:
        def decorator(func: Callable[[T], Awaitable[OutgoingAction]]) -> Callable[[T], Awaitable[OutgoingAction]]:
            self._handlers[event] = func
            return func

        return decorator

    async def handle(self, event: IncomingEvent) -> OutgoingAction:

        if self._cd_manager.check_all_cooldown(event.event):
            return OutgoingAction(
                action=Action.IGNORE,
                data={"text": "Aguardando cooldown"},
            )

        handler = self._handlers.get(event.event)

        if not handler:
            raise HTTPException(status_code=404, detail="Evento não encontrado")

        self._cd_manager.add_cooldown(event.event, 1200) # Individual cd, 20 min
        self._cd_manager.add_cooldown("GLOBAL_COOLDOWN", random.randint(30, 60)) # Global cd, 30-60 sec

        return await handler(event)


event_handler = EventHandler()


# Ideia de implementação:
# @app.post("/event")
# async def handle_event(event: IncomingEvent):
#     return handler.handle(event)

# @handler.register(Event.ITEM_CRAFTED)
# async def handle_item_craft(event_detail: ItemCraftedEventData):
#     return OutgoingAction(Action.SEND_CHAT, {"text": f"Item com ID {event_detail.item} foi criado"})
