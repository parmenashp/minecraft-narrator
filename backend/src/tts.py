import asyncio
import subprocess
import threading
import os
from typing import Generator, Iterator
from elevenlabs import generate, set_api_key
from loguru import logger

from src.models import Action, OutgoingAction
from src.websocket import ws
from src.config import global_config
from src.queue import Queue
from src.utils import singleton


@singleton
class TTS:
    def __init__(self):
        self.voice_id = ""
        self.is_playing = False
        self.queue: Queue[Generator] = Queue(maxsize=2)

        if not os.path.isfile("mpv.exe"):
            logger.warning("mpv.exe not found, TTS disabled")
            global_config.tts = False

        if global_config.elevenlabs_api_key != "":
            set_api_key(global_config.elevenlabs_api_key)
        else:
            global_config.tts = False

        if global_config.elevenlabs_voice_id != "":
            self.voice_id = global_config.elevenlabs_voice_id
        else:
            global_config.tts = False

    def synthesize(self, gen: Generator[str, None, None], loop: asyncio.AbstractEventLoop) -> None:
        self.queue.put(gen)
        if not self.is_playing:
            self.is_playing = True
            next_gen = self.queue.get()
            self.play_next(next_gen, loop)
        else:
            logger.debug("TTS already playing, added to queue")

    def play_next(self, text: Generator[str, None, None], loop: asyncio.AbstractEventLoop) -> None:
        logger.debug("Playing next")
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

        def non_stream():
            nonlocal full_text
            try:
                t = "".join([chunk for chunk in text])
                full_text = t
                return t
            finally:
                generator_done.set()
        logger.info(f"Using {global_config.elevenlabs_buffer_size} chatgpt buffer size")
        gen = generate(
            text=wrapped_generator() if global_config.elevenlabs_streaming else non_stream(),
            voice=self.voice_id,
            stream=global_config.elevenlabs_streaming,
            model="eleven_multilingual_v2",
            stream_chunk_size=global_config.elevenlabs_buffer_size,
        )
        stream_thread = threading.Thread(
            target=self.stream,
            kwargs={"audio": gen, "loop": loop},
        )
        stream_thread.start()
        generator_done.wait()
        if full_text == "":
            response = OutgoingAction(
                action=Action.IGNORE,
                data="Não foi possível gerar texto",
            )
            self.finished_playing(loop)
        else:
            response = OutgoingAction(
                action=Action.SEND_CHAT,
                data=full_text,
            )
        asyncio.run_coroutine_threadsafe(
            ws.broadcast(response.model_dump()),
            loop,
        )

    def finished_playing(self, loop: asyncio.AbstractEventLoop):
        if len(self.queue.all()) > 0:
            self.is_playing = True
            next_gen = self.queue.get()
            self.play_next(next_gen, loop)
        else:
            self.is_playing = False

    def stream(self, audio: Iterator[bytes] | bytes, loop: asyncio.AbstractEventLoop):
        audio_stream = iter([audio]) if isinstance(audio, bytes) else audio
        assert isinstance(audio_stream, Iterator)

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
