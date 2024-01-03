import fastapi

from src import models
from src.models import Event, Action, OutgoingAction, IncomingEvent, Pong, Config
from src.handler import event_handler
from src.chatgpt import chat
from src.config import GlobalConfig

app = fastapi.FastAPI()

global_config = GlobalConfig()


@app.post("/event")
async def handle_event(event: IncomingEvent) -> OutgoingAction:
    print("in:", event)
    r = await event_handler.handle(event, global_config)
    if r.action == Action.IGNORE:
        return r
    text = r.data["text"]
    chat_response = await chat.ask(text)
    r.data["text"] = chat_response
    print("out:", r)
    return r


@app.get("/ping")
async def ping():
    return Pong(text="pong")


@app.post("/config")
async def config(req_config: Config):
    global_config.set_cooldown_individual(req_config.cooldown_individual)
    global_config.set_cooldown_global(req_config.cooldown_global)
    print("config.py:", global_config)
    return


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
    if event.data["tool"] == "block.minecraft.air":
        event.data["tool"] = "as próprias mãos"

    text = f'Jogador "Felps" quebrou o bloco "{event.data["block"]}" com "{event.data["tool"]}"'
    chat_response = text
    return OutgoingAction(
        action=Action.SEND_CHAT,
        data={"text": chat_response},
    )


@event_handler.register(Event.BLOCK_PLACED)
async def handle_block_placed(event: IncomingEvent[models.BlockPlacedEventData]):
    text = f'Jogador "Felps" colocou o bloco "{event.data["block"]}"'
    # chat_response = await chat.ask(text)
    chat_response = text
    return OutgoingAction(
        action=Action.SEND_CHAT,
        data={"text": chat_response},
    )


@event_handler.register(Event.PLAYER_DEATH)
async def handle_player_death(event: IncomingEvent[models.PlayerDeathEventData]):
    text = f'Jogador "Felps" morreu por "{event.data["cause"]}"'
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
    chat_response = text
    return OutgoingAction(
        action=Action.SEND_CHAT,
        data={"text": chat_response},
    )


@event_handler.register(Event.MOB_KILLED)
async def handle_mob_killed(event: IncomingEvent[models.MobKilledEventData]):
    if event.data["weapon"] == "block.minecraft.air":
        event.data["weapon"] = "as próprias mãos"
    text = f'Jogador "Felps" matou "{event.data["mob"]}" com "{event.data["weapon"]}"'
    chat_response = text
    return OutgoingAction(
        action=Action.SEND_CHAT,
        data={"text": chat_response},
    )


@event_handler.register(Event.DIMENSION_CHANGED)
async def handle_dimension_changed(event: IncomingEvent[models.DimensionChangedEventData]):
    return OutgoingAction(
        action=Action.SEND_CHAT,
        data={"text": f'Jogador "Felps" entrou na dimensão "{event.data["dimension"]}"'},
    )
