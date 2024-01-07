import threading
from typing import Generator
from elevenlabs import generate, set_api_key
import os

from src.audio import stream

if "ELEVENLABS_API_KEY" in os.environ:
    print("Using Eleven Labs API key")
    set_api_key(os.environ["ELEVENLABS_API_KEY"])
else:
    print("You must set ELEVENLABS_API_KEY environment variable")
if "ELEVENLABS_VOICE_ID" in os.environ:
    print("Using Eleven Labs voice ID: " + os.environ["ELEVENLABS_VOICE_ID"])
    voice_id = os.environ["ELEVENLABS_VOICE_ID"]
else:
    print("You must set ELEVENLABS_VOICE_ID environment variable")


def tts(text: Generator):
    full_text = ""
    generator_done = threading.Event()

    def wrapped_generator():
        nonlocal full_text
        for chunk in text:
            full_text += chunk
            yield chunk
        generator_done.set()

    gen = generate(text=wrapped_generator(), voice=voice_id, stream=True, model="eleven_multilingual_v2")
    stream_thread = threading.Thread(target=stream, kwargs={"audio_stream": gen})

    stream_thread.start()

    generator_done.wait()
    return full_text
