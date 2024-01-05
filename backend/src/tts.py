from elevenlabs import generate, set_api_key
import os

from src.audio import stream

if "ELEVENLABS_API_KEY" in os.environ:
    print("Using Eleven Labs API key")
    set_api_key(os.environ["ELEVENLABS_API_KEY"])


def tts(text):
    stream(generate(text=text, voice="uC0NOoj2RDmLQrFl1r40", stream=True, model="eleven_multilingual_v2"))
