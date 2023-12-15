import fastapi

from src import models
from src.models import Event, Action, OutgoingAction, IncomingEvent
from src.handler import event_handler

app = fastapi.FastAPI()


@app.post("/event")
async def handle_event(event: IncomingEvent) -> OutgoingAction:
    print("in:", event)
    r = await event_handler.handle(event)
    print("out", r)
    return r


@event_handler.register(Event.ITEM_CRAFTED)
async def handle_item_crafted(event: IncomingEvent[models.ItemCraftedEventData]):
    return OutgoingAction(
        action=Action.SEND_CHAT,
        data={"text": f"Item com ID {event.data['item']} foi criado com quantidade {event.data['amount']}"},
    )


@event_handler.register(Event.BLOCK_BROKEN)
async def handle_block_broken(event: IncomingEvent[models.BlockBrokenEventData]):
    return OutgoingAction(
        action=Action.SEND_CHAT,
        data={"text": f"Block com ID {event.data['block']} foi quebrado com ferramenta {event.data['tool']}"},
    )


@event_handler.register(Event.PLAYER_DEATH)
async def handle_player_death(event: IncomingEvent[models.PlayerDeathEventData]):
    return OutgoingAction(
        action=Action.SEND_CHAT,
        data={"text": f"Player morreu por {event.data['cause']}"},
    )


@event_handler.register(Event.ADVANCEMENT)
async def handle_achievement(event: IncomingEvent[models.AdvancementEventData]):
    return OutgoingAction(
        action=Action.SEND_CHAT,
        data={"text": f"Achievement {event.data['advancement']} foi desbloqueado"},
    )
