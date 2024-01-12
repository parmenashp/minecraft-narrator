import os
from dataclasses import dataclass, fields
from dotenv import load_dotenv
from termcolor import colored, cprint

load_dotenv()


def redact(name: str, value: str) -> str:
    # if ends with _KEY, redact
    if name.endswith("_KEY"):
        return "[REDACTED]"
    else:
        return value


def env_or_default(name, default="") -> str:
    if name in os.environ:
        cprint(colored(f"Using env var {name}: ", "green") + redact(name, os.environ[name]))
        return os.environ[name]

    if default == "":
        cprint(
            colored(f"Attention needed: missing env var {name}, configure on mod configuration screen in minecraft",
                    "red"))
    else:
        cprint(colored(f"Using default value for {name}: ", "yellow") + default)
    return default


@dataclass
class GlobalConfig:
    cooldown_global: int = int(env_or_default("COOLDOWN_GLOBAL", "30"))
    cooldown_individual: int = int(env_or_default("COOLDOWN_INDIVIDUAL", "5"))
    tts: bool = env_or_default("TTS", "true").lower() == "true"

    openai_api_key: str = env_or_default("OPENAI_API_KEY")
    openai_base_url: str = env_or_default("OPENAI_BASE_URL", "https://api.openai.com/v1")
    openai_model: str = env_or_default("OPENAI_MODEL", "gpt-4-1106-preview")

    elevenlabs_api_key: str = env_or_default("ELEVENLABS_API_KEY")
    elevenlabs_voice_id: str = env_or_default("ELEVENLABS_VOICE_ID")

    def set_all(self, config):
        attributes = [f.name for f in fields(self)]
        for attribute in attributes:
            value = getattr(config, attribute, None)
            if value is not None:
                setattr(self, attribute, value)

    def save(self):
        with open(".env", "w") as f:
            attributes = [f.name for f in fields(self)]
            for attribute in attributes:
                value = getattr(self, attribute, None)
                if value is not None:
                    f.write(f"{attribute.upper()}={value}\n")


global_config = GlobalConfig()
