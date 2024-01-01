class GlobalConfig:
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(GlobalConfig, cls).__new__(cls)
            cls._instance.cooldown_individual = 5
            cls._instance.cooldown_global = 30
        return cls._instance
