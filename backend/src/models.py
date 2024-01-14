from pydantic import BaseModel
from enum import StrEnum
from typing import Generic, TypeVar


class BaseEventData(BaseModel):
    pass


DataT = TypeVar("DataT", bound=BaseEventData)


class Event(StrEnum):
    ITEM_CRAFTED = "item_crafted"
    BLOCK_BROKEN = "block_broken"
    BLOCK_PLACED = "block_placed"
    PLAYER_DEATH = "player_death"
    ADVANCEMENT = "advancement"
    ITEM_PICKUP = "item_pickup"
    MOB_KILLED = "mob_killed"
    DIMENSION_CHANGED = "dimension_changed"
    PLAYER_CHAT = "player_chat"
    PLAYER_ATE = "player_ate"
    JOIN_WORLD = "join_world"


class ItemCraftedEventData(BaseEventData):
    item: str
    amount: int


class BlockBrokenEventData(BaseEventData):
    block: str
    tool: str


class BlockPlacedEventData(BaseEventData):
    block: str


class PlayerDeathEventData(BaseEventData):
    cause: str


class AdvancementEventData(BaseEventData):
    advancement: str


class ItemPickupEventData(BaseEventData):
    item: str
    amount: int


class MobKilledEventData(BaseEventData):
    mob: str
    weapon: str


class DimensionChangedEventData(BaseEventData):
    dimension: str


class PlayerChatEventData(BaseEventData):
    message: str


class JoinWorldEventData(BaseEventData):
    world: str


class PlayerAteEventData(BaseEventData):
    item: str


class IncomingEvent(BaseModel, Generic[DataT]):
    event: Event
    data: dict


class Pong(BaseModel, Generic[DataT]):
    text: str


class Config(BaseModel, Generic[DataT]):
    elevenlabs_api_key: str
    elevenlabs_voice_id: str
    openai_api_key: str
    openai_base_url: str
    openai_model: str
    cooldown_individual: int
    cooldown_global: int
    narrator_volume: int
    tts: bool


# ==== Outgoing ====
class Action(StrEnum):
    IGNORE = "ignore"
    CANCEL_EVENT = "cancel_event"
    SEND_CHAT = "send_chat"


class OutgoingAction(BaseModel):
    action: str
    data: dict
