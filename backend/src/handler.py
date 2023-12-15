import asyncio
from fastapi import HTTPException
from src.models import IncomingEvent, Event, OutgoingAction
from functools import wraps
from typing import Callable, TypeVar, Awaitable

T = TypeVar("T", bound=IncomingEvent)


class EventHandler:
    def __init__(self):
        self._handlers = {}

    def register(
        self, event: Event
    ) -> Callable[[Callable[[T], Awaitable[OutgoingAction]]], Callable[[T], Awaitable[OutgoingAction]]]:
        def decorator(func: Callable[[T], Awaitable[OutgoingAction]]) -> Callable[[T], Awaitable[OutgoingAction]]:
            self._handlers[event] = func
            return func

        return decorator

    async def handle(self, event: IncomingEvent) -> OutgoingAction:
        handler = self._handlers.get(event.event)
        if not handler:
            raise HTTPException(status_code=404, detail="Evento não encontrado")
        return await handler(event)


event_handler = EventHandler()


# Ideia de implementação:
# @app.post("/event")
# async def handle_event(event: IncomingEvent):
#     return handler.handle(event)

# @handler.register(Event.ITEM_CRAFTED)
# async def handle_item_craft(event_detail: ItemCraftedEventData):
#     return OutgoingAction(Action.SEND_CHAT, {"text": f"Item com ID {event_detail.item} foi criado"})
