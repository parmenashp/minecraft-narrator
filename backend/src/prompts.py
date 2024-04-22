import json
import os
from loguru import logger
from src.config import global_config
from src.utils import singleton
from src.context import context


@singleton
class PromptManager:
    def __init__(self, ):
        self.prompts = {}
        self.personalities = {}
        self.current_personality_id = "waldemar"
        self.current_prompt_id: str = "prompt0"
        if os.path.exists("prompts.json"):
            self.load()

    def get_current_prompt(self):
        formatted = [
            {
                "role": "system",
                "content": self.prompts[self.current_prompt_id],
            }
        ]
        return formatted

    def set_current_prompt(self, prompt_id: str, clear_context: bool = False):
        if prompt_id not in self.prompts:
            logger.warning(f"{prompt_id} is not a valid promptID, nothing changed")
            return
        self.current_prompt_id = prompt_id
        if clear_context:
            context.clear()
        self.save()
        logger.info(f"Current prompt set to {prompt_id}")

    def new_custom_prompt(self, prompt_id: str, prompt: str):
        self.prompts[prompt_id] = prompt
        logger.info(f"New prompt {prompt_id} added")
        self.save()

    def save(self):
        with open("prompts.json", "w", encoding="utf-8") as f:
            obj = {
                "prompt_config": {
                    "prompts": self.prompts,
                    "current_prompt_id": self.current_prompt_id,
                },
                "personalities_config": {
                    "personalities": self.personalities,
                    "current_personality_id": self.current_personality_id,
                },
            }
            json.dump(obj, f, indent=2, ensure_ascii=False)
            logger.info("Prompts saved")

    def load(self):
        try:
            with open("prompts.json", "r", encoding="utf-8") as f:
                obj = json.load(f)
                self.prompts = obj["prompt_config"]["prompts"]
                self.current_prompt_id = obj["prompt_config"]["current_prompt_id"]
                self.personalities = obj["personalities_config"]["personalities"]
                self.current_personality_id = obj["personalities_config"]["current_personality_id"]
        except:
            pass

    def set_personality(self, personality_id: str, clear_context):

        self.current_personality_id = personality_id
        self.set_current_prompt(self.personalities[personality_id]["prompt_id"], clear_context)
        global_config.elevenlabs_voice_id = self.personalities[personality_id]["voice_id"]
        global_config.elevenlabs_model = self.personalities[personality_id]["model"]
        self.save()

        logger.info(f"Current personality set to {personality_id}")


prompt_manager = PromptManager()
