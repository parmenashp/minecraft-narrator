import asyncio
import os
import timeit
from loguru import logger
from src.config import global_config
from src.models import Config
from src.tts import tts
from src.chatgpt import chat
from dotenv import load_dotenv

load_dotenv()
system_prompt = [
    {
        "role": "system",
        "content": """
        Este é um teste de chatbot.
        Você deve responder com 80 palavras.
        Responda de acordo com as regras do teste.
        """,
    }
]


def redact(name: str, value: str) -> str:
    # if ends with _KEY, redact
    if name.endswith("_key"):
        return "[REDACTED]"
    return value


def run(prompt: str, loop):
    gen = chat.ask(prompt, system_prompt=system_prompt)
    tts.synthesize(gen, loop)


def test1(loop):
    config = Config(
        cooldown_global=0,
        cooldown_individual=0,
        narrator_volume=100,
        elevenlabs_api_key=os.getenv("ELEVENLABS_API_KEY", ""),
        elevenlabs_voice_id=os.getenv("ELEVENLABS_VOICE_ID", ""),
        elevenlabs_streaming=True,
        elevenlabs_buffer_size=1024,
        chatgpt_buffer_size=10,
        openai_streaming=True,
        openai_api_key=os.getenv("OPENAI_API_KEY", ""),
        openai_base_url=os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1"),
        openai_model=os.getenv("OPENAI_MODEL", "gpt-3.5-turbo"),
        tts=True,
    )
    global_config.set_all(config)
    s_config = ", ".join(f"{key}={redact(key, str(value))}" for key, value in config.model_dump().items())
    logger.info(f"Starting test with config: {s_config}")
    run("Teste de chatbot com elevenlabs_buffer_size=1024, chatgpt_buffer_size=10, streaming ativado", loop)
    logger.info("Teste finalizado")


def test2(loop):
    config = Config(
        cooldown_global=0,
        cooldown_individual=0,
        narrator_volume=100,
        elevenlabs_api_key=os.getenv("ELEVENLABS_API_KEY", ""),
        elevenlabs_voice_id=os.getenv("ELEVENLABS_VOICE_ID", ""),
        elevenlabs_streaming=True,
        elevenlabs_buffer_size=4096,
        chatgpt_buffer_size=10,
        openai_streaming=True,
        openai_api_key=os.getenv("OPENAI_API_KEY", ""),
        openai_base_url=os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1"),
        openai_model=os.getenv("OPENAI_MODEL", "gpt-3.5-turbo"),
        tts=True,
    )
    global_config.set_all(config)
    s_config = ", ".join(f"{key}={redact(key, str(value))}" for key, value in config.model_dump().items())
    logger.info(f"Starting test with config: {s_config}")
    run("Teste de chatbot com elevenlabs_buffer_size=4096, chatgpt_buffer_size=10, streaming ativado", loop)
    logger.info("Teste finalizado")


def test3(loop):
    config = Config(
        cooldown_global=0,
        cooldown_individual=0,
        narrator_volume=100,
        elevenlabs_api_key=os.getenv("ELEVENLABS_API_KEY", ""),
        elevenlabs_voice_id=os.getenv("ELEVENLABS_VOICE_ID", ""),
        elevenlabs_streaming=False,
        elevenlabs_buffer_size=1024,
        chatgpt_buffer_size=10,
        openai_streaming=False,
        openai_api_key=os.getenv("OPENAI_API_KEY", ""),
        openai_base_url=os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1"),
        openai_model=os.getenv("OPENAI_MODEL", "gpt-3.5-turbo"),
        tts=True,
    )
    global_config.set_all(config)
    s_config = ", ".join(f"{key}={redact(key, str(value))}" for key, value in config.model_dump().items())
    logger.info(f"Starting test with config: {s_config}")
    run("Teste de chatbot com elevenlabs_buffer_size=1024, chatgpt_buffer_size=10, streaming desativado", loop)
    logger.info("Teste finalizado")


async def main():
    try:
        loop = asyncio.get_event_loop()
        logger.info("Starting tests")
        start = timeit.default_timer()
        time = timeit.timeit(lambda: test1(loop), number=1)
        logger.info(f"Test 1 finished in {time} seconds")
        time = timeit.timeit(lambda: test2(loop), number=1)
        logger.info(f"Test 2 finished in {time} seconds")
        time = timeit.timeit(lambda: test3(loop), number=1)
        logger.info(f"Test 3 finished in {time} seconds")
        logger.info("Tests finished")
        stop = timeit.default_timer()
        logger.info(f"Tests finished in {stop - start} seconds")
    except Exception as e:
        logger.error("Tests finished with errors")
        logger.error(f"Error: {e}")
        raise e


asyncio.run(main())
