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


class IncomingEvent(BaseModel, Generic[DataT]):
    event: Event
    data: dict


class Pong(BaseModel, Generic[DataT]):
    text: str


class Config(BaseModel, Generic[DataT]):
    cooldown_individual: int
    cooldown_global: int


# ==== Outgoing ====
class Action(StrEnum):
    IGNORE = "ignore"
    CANCEL_EVENT = "cancel_event"
    SEND_CHAT = "send_chat"


class OutgoingAction(BaseModel):
    action: str
    data: dict
