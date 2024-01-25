import os
from dataclasses import dataclass, fields

from dotenv import load_dotenv
from loguru import logger

load_dotenv()


def redact(name: str, value: str) -> str:
    # if ends with _KEY, redact
    if name.endswith("_KEY"):
        return "[REDACTED]"
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


@dataclass
class GlobalConfig:
    cooldown_global: int = int(env_or_default("COOLDOWN_GLOBAL", "30"))
    cooldown_individual: int = int(env_or_default("COOLDOWN_INDIVIDUAL", "5"))
    tts: bool = env_or_default("TTS", "true").lower() == "true"

    elevenlabs_buffer_size: int = int(env_or_default("ELEVENLABS_BUFFER_SIZE", "2048"))
    chatgpt_buffer_size: int = int(env_or_default("CHATGPT_BUFFER_SIZE", "10"))

    openai_api_key: str = env_or_default("OPENAI_API_KEY")
    openai_base_url: str = env_or_default("OPENAI_BASE_URL", "https://api.openai.com/v1")
    openai_model: str = env_or_default("OPENAI_MODEL", "gpt-4-1106-preview")

    elevenlabs_api_key: str = env_or_default("ELEVENLABS_API_KEY")
    elevenlabs_voice_id: str = env_or_default("ELEVENLABS_VOICE_ID")

    narrator_volume: int = int(env_or_default("NARRATOR_VOLUME", "100"))

    def set_all(self, config):
        attributes = [f.name for f in fields(self)]
        for attribute in attributes:
            value = getattr(config, attribute, None)
            if value is not None and value != "":
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


global_config = GlobalConfig()
