from enum import Enum, StrEnum
from typing import List, TypedDict

from pydantic import BaseModel


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
    TIME_CHANGED = "time_changed"
    PLAYER_CHAT = "player_chat"
    PLAYER_ATE = "player_ate"
    RIDING = "riding"
    WAKE_UP = "wake_up"
    JOIN_WORLD = "join_world"
    ITEM_FISHED = "item_fished"
    ITEM_REPAIR = "item_repair"
    ANIMAL_BREED = "animal_breed"
    ITEM_TOSS = "item_toss"
    CONFIG = "config"
    CUSTOM_PROMPT = "custom_prompt"
    SET_SYSTEM = "set_system"
    CUSTOM_TTS = "custom_tts"
    VOICE_COMPLETE = "voice_complete"
    VOICE_ACTIVATE = "voice_activate"


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


class SystemPrompt(TypedDict):
    system_message: str
    interactions: List[str]


class Response(BaseModel):
    mensagem: str
    interacao: Enum

# ==== Outgoing to Minecraft ====
class Action(StrEnum):
    IGNORE = "ignore"
    SEND_CHAT = "send_chat"
    NEW_PERSONALITY = "new_personality"
    SPEECH_DATA = "speech_data"
    INTERACTION = "interaction"


class OutgoingAction(BaseModel):
    action: Action
    data: str
