from typing import Generator, cast
from openai import OpenAI, Stream
from openai.types.chat import ChatCompletion, ChatCompletionChunk
from loguru import logger

from src.config import global_config, GlobalConfig
from src.context import context
from src.prompts import prompt_manager
from src.utils import singleton

gpt_config = {
    "temperature": 1,
    "max_tokens": 256,
    "top_p": 1,
    "frequency_penalty": 0,
    "presence_penalty": 0,
    "stop": ["\n"],
}


@singleton
class ChatGPT:
    def __init__(self, api_key, base_url, model="gpt-3.5-turbo"):
        self.model = model
        self.client = OpenAI(
            api_key=api_key,
            base_url=base_url,
        )

    def ask(self, text: str, system_prompt: list[dict[str, str]] | None = None) -> Generator[str, None, None]:
        logger.debug(f"Sending prompt to GPT: {text!r}")
        user_prompt = {"role": "user", "content": text}
        if system_prompt is None:
            system_prompt = prompt_manager.get_current_prompt()
        messages: list = system_prompt + context.all() + [user_prompt]

        response: ChatCompletion | Stream[ChatCompletionChunk] = self.client.chat.completions.create(
            model=self.model,
            messages=messages,
            stream=global_config.openai_streaming,
            **gpt_config,
        )

        context.put(user_prompt)
        response_text = ""
        if global_config.openai_streaming:
            logger.debug("Using OpenAI streaming mode")
            logger.info(f"Using {global_config.chatgpt_buffer_size} chatgpt buffer size")
            response = cast(Stream[ChatCompletionChunk], response)
            buffer = ""
            for chunk in response:
                delta = chunk.choices[0].delta.content
                if delta:
                    response_text += delta
                    buffer += delta
                    if len(buffer) >= global_config.chatgpt_buffer_size:
                        logger.debug(f"Yielding GPT response: {buffer!r}")
                        yield buffer
                        buffer = ""
            if buffer:
                logger.debug(f"Yielding GPT response: {buffer!r}")
                yield buffer

        else:
            logger.debug("Using OpenAI non-streaming mode")
            response = cast(ChatCompletion, response)
            response_text = response.choices[0].message.content
            if response_text:
                logger.debug(f"Yielding GPT response: {response_text!r}")
                yield response_text

        context.put({"role": "assistant", "content": response_text})

    def set_config(self, config: GlobalConfig):
        self.client.api_key = config.openai_api_key
        self.client.base_url = config.openai_base_url
        self.model = config.openai_model


chat = ChatGPT(
    global_config.openai_api_key,
    global_config.openai_base_url,
    global_config.openai_model,
)
