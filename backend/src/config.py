from dataclasses import dataclass


@dataclass
class GlobalConfig:
    cooldown_global: int = 30
    cooldown_individual: int = 5
