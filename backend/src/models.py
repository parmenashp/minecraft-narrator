from enum import StrEnum

from pydantic import BaseModel


class BaseEventData(BaseModel):
    pass


class Event(StrEnum):
    ITEM_CRAFTED = "item_crafted"
    BLOCK_BROKEN = "block_broken"
    BLOCK_PLACED = "block_placed"
    PLAYER_DEATH = "player_death"
    ADVANCEMENT = "advancement"
    ITEM_PICKUP = "item_pickup"
    CHEST_CHANGE = "chest_change"
    ITEM_SMELTED = "item_smelted"
    MOB_KILLED = "mob_killed"
    DIMENSION_CHANGED = "dimension_changed"
    PLAYER_CHAT = "player_chat"
    PLAYER_ATE = "player_ate"
    RIDING = "riding"
    WAKE_UP = "wake_up"
    JOIN_WORLD = "join_world"
    ITEM_FISHED = "item_fished"
    ITEM_REPAIR = "item_repair"
    CONFIG = "config"


class IncomingEvent(BaseModel):
    event: Event
    data: str


class Config(BaseModel):
    elevenlabs_api_key: str
    elevenlabs_voice_id: str
    elevenlabs_streaming: bool
    openai_streaming: bool
    openai_api_key: str
    openai_base_url: str
    openai_model: str
    elevenlabs_buffer_size: int
    chatgpt_buffer_size: int
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
    data: str
