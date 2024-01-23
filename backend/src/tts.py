import asyncio
import subprocess
import threading
from typing import Generator, Iterator
from elevenlabs import generate, set_api_key
import os
from src.models import Action, OutgoingAction
from termcolor import colored, cprint

from src.websocket import ws
from src.config import global_config
from src.queue import Queue

class TTS:
    def __init__(self):
        self.voice_id = ""
        self.is_playing = False
        self.queue: Queue[Generator] = Queue(maxsize=2)

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

    def synthesize(self, gen: Generator, loop) -> None:
        self.queue.put(gen)
        if not self.is_playing:
            self.is_playing = True
            next_gen = self.queue.get()
            self.play_next(next_gen, loop)
        else:
            print("TTS already playing, added to queue")

    def play_next(self, text: Generator, loop: asyncio.AbstractEventLoop) -> None:
        print("Playing next")

        if global_config.tts is False:
            try:
                full_text = "".join([chunk for chunk in text])
                response = OutgoingAction(
                    action=Action.SEND_CHAT,
                    data=full_text,
                )
                asyncio.run_coroutine_threadsafe(ws.broadcast(response.model_dump()), loop)
            finally:
                self.finished_playing(loop)
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
            stream_chunk_size=global_config.elevenlabs_buffer_size,
        )
        stream_thread = threading.Thread(
            target=self.stream,
            kwargs={"audio_stream": gen, "loop": loop},
        )
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
        asyncio.run_coroutine_threadsafe(
            ws.broadcast(response.model_dump()),
            loop,
        )

    def finished_playing(self, loop):
        if len(self.queue.all()) > 0:
            self.is_playing = True
            next_gen = self.queue.get()
            self.play_next(next_gen, loop)
        else:
            self.is_playing = False

    def stream(self, audio_stream: Iterator[bytes], loop):
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
            self.finished_playing(loop)

    def set_config(self, config):
        self.voice_id = config.elevenlabs_voice_id
        set_api_key(config.elevenlabs_api_key)


tts = TTS()
