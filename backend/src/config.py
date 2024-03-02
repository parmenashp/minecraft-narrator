import os
from dataclasses import dataclass, fields
from typing import Optional

from dotenv import load_dotenv
from loguru import logger
from src.utils import singleton

load_dotenv(".env", override=True)


def redact(name: str, value: str) -> str:
    # if ends with _KEY, redact
    if name.endswith("_KEY"):
        return f"{value[:3]}...{value[-3:]}"
    else:
        return value


def env_or_default(name, default="") -> str:
    if name in os.environ:
        logger.info(f"Using env var {name}: {redact(name, os.environ[name])}")
        return os.environ[name]

    if default == "":
        logger.error(f"Missing env var {name}, configure on mod configuration screen in minecraft")
    else:
        logger.info(f"Using default value for {name}: {default}")
    return default


@singleton
@dataclass
class GlobalConfig:
    cooldown_global: int = int(env_or_default("COOLDOWN_GLOBAL", "30"))
    cooldown_individual: int = int(env_or_default("COOLDOWN_INDIVIDUAL", "5"))
    tts: bool = env_or_default("TTS", "true").lower() == "true"

    elevenlabs_buffer_size: int = int(env_or_default("ELEVENLABS_BUFFER_SIZE", "2048"))
    chatgpt_buffer_size: int = int(env_or_default("CHATGPT_BUFFER_SIZE", "10"))

    openai_streaming: bool = env_or_default("OPENAI_STREAMING", "false").lower() == "true"
    openai_api_key: str = env_or_default("OPENAI_API_KEY")
    openai_base_url: str = env_or_default("OPENAI_BASE_URL", "https://api.openai.com/v1")
    openai_model: str = env_or_default("OPENAI_MODEL", "gpt-4-1106-preview")

    elevenlabs_streaming: bool = env_or_default("ELEVENLABS_STREAMING", "false").lower() == "true"
    elevenlabs_api_key: str = env_or_default("ELEVENLABS_API_KEY")
    elevenlabs_voice_id: str = env_or_default("ELEVENLABS_VOICE_ID")
    elevenlabs_model: Optional[str] = env_or_default("ELEVENLABS_MODEL", "eleven_multilingual_v2")

    voice_stability: Optional[float] = float(env_or_default("VOICE_STABILITY", "0.30"))
    voice_similarity_boost: Optional[float] = float(env_or_default("VOICE_SIMILARITY_BOOST", "0.75"))
    voice_style: Optional[float] = float(env_or_default("VOICE_STYLE", "0.40"))

    hypertranslate: Optional[bool] = env_or_default("HYPERTRANSLATE", "false").lower() == "true"

    narrator_volume: int = int(env_or_default("NARRATOR_VOLUME", "100"))

    discord_webhook_key: Optional[str] = env_or_default("DISCORD_WEBHOOK_KEY")

    def set_all(self, config):
        attributes = [f.name for f in fields(self)]
        for attribute in attributes:
            value = getattr(config, attribute, None)
            if value is not None and value != "":
                logger.info(f"Setting config value for {attribute}: {redact(attribute.upper(), value)}")
                setattr(self, attribute, value)
            else:
                logger.warning(f"Config value for {attribute} is empty, skipping")

    def save(self):
        logger.debug("Saving config to .env")
        with open(".env", "w") as f:
            attributes = [f.name for f in fields(self)]
            for attribute in attributes:
                value = getattr(self, attribute, None)
                if value is not None:
                    f.write(f"{attribute.upper()}={value}\n")
        logger.info("Saved config to .env")

    def as_markdown(self):
        attributes = [f.name for f in fields(self)]

        return "\n".join(
            [
                f"- **{attribute.upper()}**: `{redact(attribute.upper(), getattr(self, attribute))}`"
                for attribute in attributes
            ]
        )


global_config = GlobalConfig()
