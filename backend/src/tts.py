import threading
from typing import Generator
from elevenlabs import generate, set_api_key
import os

from src.audio import stream


class TTS:
    def __init__(self):
        self.tts_disabled = False
        self.voice_id = None

        if "ELEVENLABS_API_KEY" in os.environ:
            print("Using Eleven Labs API key")
            set_api_key(os.environ["ELEVENLABS_API_KEY"])
        else:
            print("ELEVENLABS_API_KEY not set, TTS disabled")
            self.tts_disabled = True

        if "ELEVENLABS_VOICE_ID" in os.environ:
            print("Using Eleven Labs voice ID: " + os.environ["ELEVENLABS_VOICE_ID"])
            self.voice_id = os.environ["ELEVENLABS_VOICE_ID"]
        else:
            print("ELEVENLABS_VOICE_ID not set, TTS disabled")
            self.tts_disabled = True

    def synthesize(self, text: Generator) -> str:
        if self.tts_disabled:
            return "".join([chunk for chunk in text])

        full_text = ""
        generator_done = threading.Event()

        def wrapped_generator():
            nonlocal full_text
            for chunk in text:
                full_text += chunk
                yield chunk
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


tts = TTS()
