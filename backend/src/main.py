import fastapi

from src import models
from src.models import Event, Action, OutgoingAction, IncomingEvent, Pong, Config
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
    return chat.ask(text)


@event_handler.register(Event.ITEM_CRAFTED)
def handle_item_crafted(event: IncomingEvent[models.ItemCraftedEventData]):
    text = f'Jogador "Felps" craftou o item "{event.data["item"]}"'
    return OutgoingAction(
        action=Action.SEND_CHAT,
        data={"text": text},
    )


@event_handler.register(Event.BLOCK_BROKEN)
def handle_block_broken(event: IncomingEvent[models.BlockBrokenEventData]):
    text = f'Jogador "Felps" quebrou "{event.data["block"]}" com "{event.data["tool"]}"'
    return OutgoingAction(
        action=Action.SEND_CHAT,
        data={"text": text},
    )


@event_handler.register(Event.BLOCK_PLACED)
def handle_block_placed(event: IncomingEvent[models.BlockPlacedEventData]):
    text = f'Jogador "Felps" colocou "{event.data["block"]}"'
    return OutgoingAction(
        action=Action.SEND_CHAT,
        data={"text": text},
    )


@event_handler.register(Event.PLAYER_DEATH)
def handle_player_death(event: IncomingEvent[models.PlayerDeathEventData]):
    text = f'Jogador morreu "{event.data["cause"]}"'
    return OutgoingAction(
        action=Action.SEND_CHAT,
        data={"text": text},
    )


@event_handler.register(Event.ADVANCEMENT)
def handle_achievement(event: IncomingEvent[models.AdvancementEventData]):
    text = f'Jogador "Felps" ganhou a conquista "{event.data["advancement"]}"'
    return OutgoingAction(
        action=Action.SEND_CHAT,
        data={"text": text},
    )


@event_handler.register(Event.ITEM_PICKUP)
def handle_item_pickup(event: IncomingEvent[models.ItemPickupEventData]):
    text = f'Jogador "Felps" pegou {event.data["amount"]} "{event.data["item"]}"'
    return OutgoingAction(
        action=Action.SEND_CHAT,
        data={"text": text},
    )


@event_handler.register(Event.MOB_KILLED)
def handle_mob_killed(event: IncomingEvent[models.MobKilledEventData]):
    if event.data["weapon"] == "block.minecraft.air":
        event.data["weapon"] = "as próprias mãos"
    text = f'Jogador "Felps" matou "{event.data["mob"]}" com "{event.data["weapon"]}"'
    return OutgoingAction(
        action=Action.SEND_CHAT,
        data={"text": text},
    )


@event_handler.register(Event.DIMENSION_CHANGED)
def handle_dimension_changed(event: IncomingEvent[models.DimensionChangedEventData]):
    text = f'Jogador "Felps" entrou na dimensão "{event.data["dimension"]}"'
    return OutgoingAction(
        action=Action.SEND_CHAT,
        data={"text": text},
    )


@event_handler.register(Event.PLAYER_CHAT)
def handle_player_chat(event: IncomingEvent[models.PlayerChatEventData]):
    text = f'Jogador "Felps" escreveu no chat do jogo "{event.data["message"]}"'
    return OutgoingAction(
        action=Action.SEND_CHAT,
        data={"text": text},
    )


@event_handler.register(Event.PLAYER_ATE)
def handle_player_ate(event: IncomingEvent[models.PlayerAteEventData]):
    text = f'Jogador "Felps" comeu "{event.data["item"]}"'
    return OutgoingAction(
        action=Action.SEND_CHAT,
        data={"text": text},
    )


@event_handler.register(Event.JOIN_WORLD)
def handle_join_world(event: IncomingEvent[models.JoinWorldEventData]):
    text = f'Jogador "Felps" entrou no mundo "{event.data["world"]}"'
    return OutgoingAction(
        action=Action.SEND_CHAT,
        data={"text": text},
    )
