from pydantic import BaseModel
from enum import StrEnum


class BaseEventData(BaseModel):
    pass


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


class IncomingEvent(BaseModel):
    event: Event
    data: str


class Pong(BaseModel):
    text: str


class Config(BaseModel):
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
    SEND_CHAT = "send_chat"


class OutgoingAction(BaseModel):
    action: Action
    data: dict
