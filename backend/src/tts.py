import threading
from typing import Generator
from elevenlabs import generate, set_api_key
import os
from termcolor import colored, cprint

from src.audio import stream
from src.config import global_config


class TTS:
    def __init__(self):
        self.voice_id = ""

        if not os.path.isfile("mpv.exe"):
            cprint(colored("mpv.exe not found, TTS disabled", "red"))
            global_config.tts = False

        if global_config.elevenlabs_api_key != "":
            set_api_key(global_config.elevenlabs_api_key)
        else:
            global_config.tts = False

        if global_config.elevenlabs_voice_id != "":
            self.voice_id = global_config.elevenlabs_voice_id
        else:
            global_config.tts = False

    def synthesize(self, text: Generator) -> str:
        if global_config.tts is False:
            return "".join([chunk for chunk in text])

        full_text = ""
        generator_done = threading.Event()

        def wrapped_generator():
            nonlocal full_text
            try:
                for chunk in text:
                    full_text += chunk
                    yield chunk
            finally:
                generator_done.set()

        gen = generate(
            text=wrapped_generator(),
            voice=self.voice_id,
            stream=True,
            model="eleven_multilingual_v2",
        )
        stream_thread = threading.Thread(target=stream, kwargs={"audio_stream": gen})
        stream_thread.start()

        generator_done.wait()
        return full_text

    def set_config(self, config):
        self.voice_id = config.elevenlabs_voice_id
        set_api_key(config.elevenlabs_api_key)


tts = TTS()
