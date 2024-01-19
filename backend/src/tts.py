import subprocess
import threading
from typing import Generator, Iterator
from elevenlabs import generate, set_api_key
import os
from src.models import Action, OutgoingAction
from termcolor import colored, cprint

from src.websocket import ws
from src.config import global_config


class TTS:
    def __init__(self):
        self.voice_id = ""
        self.is_playing = False
        self.queue: list[Generator] = []

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

    def synthesize(self, gen: Generator) -> None:
        self.queue.append(gen)
        if not self.is_playing:
            self.is_playing = True
            self.play_next(gen)

    def play_next(self, text: Generator) -> None:
        print("Playing next")
        self.queue.remove(text)

        if global_config.tts is False:
            full_text = "".join([chunk for chunk in text])
            response = OutgoingAction(
                action=Action.SEND_CHAT,
                data=full_text,
            )
            ws.sync_broadcast(response.model_dump())
            return

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
        stream_thread = threading.Thread(target=self.stream, kwargs={"audio_stream": gen})
        stream_thread.start()
        generator_done.wait()
        if full_text == "":
            response = OutgoingAction(
                action=Action.IGNORE,
                data="Não foi possível gerar texto",
            )
            self.is_playing = False
        else:
            response = OutgoingAction(
                action=Action.SEND_CHAT,
                data=full_text,
            )
        ws.sync_broadcast(response.model_dump())

    def finished_playing(self):
        if len(self.queue) > 0:
            self.is_playing = True
            self.play_next(self.queue[0])
        else:
            self.is_playing = False

    def stream(self, audio_stream: Iterator[bytes]):
        try:
            mpv_command = [
                "./mpv.exe",
                "--no-cache",
                f"--volume={global_config.narrator_volume}",
                "--no-terminal",
                "--",
                "fd://0",
            ]
            mpv_process = subprocess.Popen(
                mpv_command,
                stdin=subprocess.PIPE,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
            )

            for chunk in audio_stream:
                if mpv_process.stdin and chunk:
                    mpv_process.stdin.write(chunk)
                    mpv_process.stdin.flush()

            if mpv_process.stdin:
                mpv_process.stdin.close()
            mpv_process.wait()
        finally:
            self.finished_playing()

    def set_config(self, config):
        self.voice_id = config.elevenlabs_voice_id
        set_api_key(config.elevenlabs_api_key)


tts = TTS()
