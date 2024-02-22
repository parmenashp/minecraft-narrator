import asyncio
import subprocess
import threading
import os
from typing import Generator, Iterator
from elevenlabs import generate, Voice, VoiceSettings, Voices, VoiceClone
from loguru import logger

from src.models import Action, OutgoingAction
from src.websocket import ws
from src.config import global_config
from src.queue import Queue
from src.utils import singleton


@singleton
class TTS:
    def __init__(self):
        self.is_playing = False
        self.queue: Queue[Generator] = Queue(maxsize=2)

        if (
            not os.path.isfile("mpv.exe")
            or not global_config.elevenlabs_api_key
            or not global_config.elevenlabs_voice_id
            or not global_config.elevenlabs_model
        ):
            logger.warning("mpv.exe or keys not found, TTS disabled")
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

        if global_config.elevenlabs_streaming is False or global_config.openai_streaming is False:
            elevenlabs_text = non_stream()
        else:
            elevenlabs_text = wrapped_generator()
        logger.info(f"Using {global_config.elevenlabs_buffer_size} elevenlabs buffer size")
        voice = Voice(
            voice_id=global_config.elevenlabs_voice_id,
            settings=VoiceSettings(
                stability=global_config.voice_stability,  # type: ignore
                similarity_boost=global_config.voice_similarity_boost,  # type: ignore
                style=global_config.voice_style,
            ),
        )

        gen = generate(
            text=elevenlabs_text,
            voice=voice,
            api_key=global_config.elevenlabs_api_key,
            stream=global_config.elevenlabs_streaming,
            model=global_config.elevenlabs_model,  # type: ignore
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

    def get_voices(self):
        os.environ["ELEVEN_API_KEY"] = global_config.elevenlabs_api_key
        voices = Voices.from_api(global_config.elevenlabs_api_key)
        return voices.items

    def clone_voice_from_files(self, voice_name: str, files: list[str]) -> tuple[str, Voice | None]:
        os.environ["ELEVEN_API_KEY"] = global_config.elevenlabs_api_key
        clone_settings = VoiceClone(
            name=voice_name,
            files=files,
        )
        logger.info(f"Cloning voice {voice_name} with files {files}")
        try:
            r = Voice.from_clone(clone_settings)
        except Exception as e:
            logger.error(f"Error cloning voice: {e}")
            return f"Error Cloning voice: {e}", None

        logger.info(f"Voice cloned successfully: {r}")
        return "Voice cloned successfully", r


tts = TTS()
