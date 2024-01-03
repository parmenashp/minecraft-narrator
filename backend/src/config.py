class GlobalConfig:
    _cooldown_global: int = 30
    _cooldown_individual: int = 5

    def __init__(self):
        self._cooldown_global = 30
        self._cooldown_individual = 5

    def get_cooldown_global(self) -> int:
        return self._cooldown_global

    def get_cooldown_individual(self) -> int:
        return self._cooldown_individual

    def set_cooldown_global(self, cooldown_global: int):
        self._cooldown_global = cooldown_global

    def set_cooldown_individual(self, cooldown_individual: int):
        self._cooldown_individual = cooldown_individual

    def __str__(self) -> str:
        return f"GlobalConfig(cooldown_global={self._cooldown_global}, cooldown_individual={self._cooldown_individual})"
