from pydantic import BaseModel
from enum import StrEnum
from typing import Generic, TypeVar


class BaseEventData(BaseModel):
    pass


DataT = TypeVar("DataT", bound=BaseEventData)


class Event(StrEnum):
    ITEM_CRAFTED = "item_crafted"
    BLOCK_BROKEN = "block_broken"
    PLAYER_DEATH = "player_death"
    ADVANCEMENT = "advancement"


class ItemCraftedEventData(BaseEventData):
    item: str
    amount: int


class BlockBrokenEventData(BaseEventData):
    block: str
    tool: str


class PlayerDeathEventData(BaseEventData):
    cause: str


class AdvancementEventData(BaseEventData):
    advancement: str


class IncomingEvent(BaseModel, Generic[DataT]):
    event: Event
    data: dict


# ==== Outgoing ====


class Action(StrEnum):
    IGNORE = "ignore"
    CANCEL_EVENT = "cancel_event"
    SEND_CHAT = "send_chat"


class OutgoingAction(BaseModel):
    action: str
    data: dict
