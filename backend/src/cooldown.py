import time
from loguru import logger

from src.models import Event


class CooldownManager:
    def __init__(self):
        self.cooldowns = {}
        self.bypass_cooldowns: list[Event] = [
            Event.PLAYER_DEATH,
            Event.ADVANCEMENT,
            Event.DIMENSION_CHANGED,
            Event.PLAYER_CHAT,
            Event.VOICE_COMPLETE,
        ]

    def add_cooldown(self, name, duration: int):
        self.cooldowns[name] = time.time() + duration

    def is_on_cooldown(self, name) -> bool:
        return time.time() < self.cooldowns.get(name, 0)

    def get_cooldown_remaining(self, name) -> int:
        remaining = self.cooldowns.get(name, 0) - time.time()
        return max(0, remaining)

    def reset_cooldown(self, name):
        self.cooldowns[name] = 0

    def check_all_cooldown(self, event: Event) -> bool:
        """
        Check all cooldowns and return True if any of them is active, False otherwise
        """
        # TODO: if is playing narration return True (hard cd)

        if event in self.bypass_cooldowns:
            logger.info(f"Bypassing cooldown for event: {event}")
            return False

        if self.is_on_cooldown("GLOBAL_COOLDOWN"):
            logger.info(f"Global cooldown active, {self.get_cooldown_remaining('GLOBAL_COOLDOWN')} seconds remaining")
            return True

        if self.is_on_cooldown(event):
            logger.info(f"Cooldown active for event: {event}, {self.get_cooldown_remaining(event)} seconds remaining")
            return True

        return False
