from calendar import c
import fastapi

from src import models
from src.models import Event, Action, OutgoingAction, IncomingEvent
from src.handler import event_handler
from src.chatgpt import chat

app = fastapi.FastAPI()


@app.post("/event")
async def handle_event(event: IncomingEvent) -> OutgoingAction:
    print("in:", event)
    r = await event_handler.handle(event)
    print("out:", r)
    return r


@app.get("/ask")
async def ask(text: str) -> str:
    return await chat.ask(text)


@event_handler.register(Event.ITEM_CRAFTED)
async def handle_item_crafted(event: IncomingEvent[models.ItemCraftedEventData]):
    text = f'Jogador "Felps" craftou o item "{event.data["item"]}"'
    # chat_response = await chat.ask(text)
    chat_response = text
    return OutgoingAction(
        action=Action.SEND_CHAT,
        data={"text": chat_response},
    )


@event_handler.register(Event.BLOCK_BROKEN)
async def handle_block_broken(event: IncomingEvent[models.BlockBrokenEventData]):
    if event.data["tool"] == "minecraft:air":
        event.data["tool"] = "as próprias mãos"

    text = f'Jogador "Felps" quebrou o bloco "{event.data["block"]}" com "{event.data["tool"]}"'
    # chat_response = await chat.ask(text)
    chat_response = text
    return OutgoingAction(
        action=Action.SEND_CHAT,
        data={"text": chat_response},
    )


@event_handler.register(Event.PLAYER_DEATH)
async def handle_player_death(event: IncomingEvent[models.PlayerDeathEventData]):
    text = f'Jogador "Felps" morreu por "{event.data["cause"]}"'
    # chat_response = await chat.ask(text)
    chat_response = text
    return OutgoingAction(
        action=Action.SEND_CHAT,
        data={"text": chat_response},
    )


@event_handler.register(Event.ADVANCEMENT)
async def handle_achievement(event: IncomingEvent[models.AdvancementEventData]):
    return OutgoingAction(
        action=Action.SEND_CHAT,
        data={"text": f'Jogador "Felps" ganhou a conquista "{event.data["advancement"]}"'},
    )


@event_handler.register(Event.ITEM_PICKUP)
async def handle_item_pickup(event: IncomingEvent[models.ItemPickupEventData]):
    text = f'Jogador "Felps" pegou {event.data["amount"]} "{event.data["item"]}"'
    # chat_response = await chat.ask(text)
    chat_response = text
    return OutgoingAction(
        action=Action.SEND_CHAT,
        data={"text": chat_response},
    )
