from typing import Type
from openai import OpenAI, Stream
from openai.types.chat import ChatCompletion, ChatCompletionChunk
from loguru import logger
import instructor

from src.models import Response
from src.config import global_config, GlobalConfig
from src.context import context
from src.prompts import prompt_manager
from src.utils import singleton


@singleton
class ChatGPT:
    def __init__(self, api_key, base_url, model="gpt-3.5-turbo"):
        self.model = model
        self.client = instructor.from_openai(
            OpenAI(
                api_key=api_key,
                base_url=base_url,
            ),
            mode=instructor.Mode.JSON,
        )

    def ask(
        self,
        text: str,
        system_prompt: list[dict[str, str]] | None = None,
        add_to_context: bool = True,
        response_model: Type[Response] = Response,
    ) -> Response | None:
        logger.debug(f"Sending prompt to GPT: {text!r}")
        user_prompt = {"role": "user", "content": text}
        if system_prompt is None:
            system_prompt = prompt_manager.get_current_prompt()

        response_model = prompt_manager.get_current_response_model()

        messages: list = system_prompt + context.all() + [user_prompt]

        response: tuple[response_model, ChatCompletion | Stream[ChatCompletionChunk]] = (
            self.client.chat.completions.create_with_completion(
                model=self.model,
                messages=messages,
                stream=False,
                response_model=response_model,
                max_retries=1,
                temperature=1,
                max_tokens=256,
                top_p=1,
                frequency_penalty=0,
                presence_penalty=0,
                stop=["\n"],
            )
        )

        struct_response, raw = response

        if add_to_context:
            context.put(user_prompt)

        logger.debug("Using OpenAI non-streaming mode")
        logger.debug(f"Yielding GPT response: {struct_response!r}")
        if add_to_context:
            if isinstance(raw, ChatCompletion):
                raw_msg = raw.choices[0].message.content
                context.put({"role": "assistant", "content": raw_msg})

        return struct_response

    def set_config(self, config: GlobalConfig):
        if self.client.client is not None:
            self.client.client.api_key = config.openai_api_key
            self.client.client.api_key = config.openai_api_key
            self.client.client.base_url = config.openai_base_url
        self.model = config.openai_model


chat = ChatGPT(
    global_config.openai_api_key,
    global_config.openai_base_url,
    global_config.openai_model,
)
