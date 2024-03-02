from typing import Callable, Generator, Iterator, cast
from openai import OpenAI, Stream
from openai.types.chat import ChatCompletion, ChatCompletionChunk, ChatCompletionMessageParam
from loguru import logger

from src.config import global_config, GlobalConfig
from src.context import context
from src.prompts import prompt_manager
from src.utils import singleton


@singleton
class ChatGPT:
    def __init__(self, api_key, base_url, model="gpt-3.5-turbo"):
        self.model = model
        self.client = OpenAI(
            api_key=api_key,
            base_url=base_url,
        )

    def completion(
        self, messages: list[ChatCompletionMessageParam], gpt_config: dict
    ) -> Callable[[], Generator[str, None, None]] | Callable[[], str | None]:
        response: ChatCompletion | Stream[ChatCompletionChunk] = self.client.chat.completions.create(
            model=self.model,
            messages=messages,
            stream=global_config.openai_streaming,
            **gpt_config,
        )

        if global_config.openai_streaming:

            def response_gen() -> Generator[str, None, None]:
                nonlocal response
                response_text = ""
                logger.debug("Using OpenAI streaming mode")
                logger.debug(f"Using {global_config.chatgpt_buffer_size} chatgpt buffer size")
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

            return response_gen

        else:

            def response_str() -> str | None:
                nonlocal response
                logger.debug("Using OpenAI non-streaming mode")
                response = cast(ChatCompletion, response)
                response_text = response.choices[0].message.content
                logger.debug(f"Yielding GPT response: {response_text!r}")
                return response_text

            return response_str

    def ask(
        self,
        text: str,
        system_prompt: list[dict[str, str]] | None = None,
        add_to_context: bool = True,
        hypertranslate: bool | None = global_config.hypertranslate,
    ) -> Generator[str, None, None] | str | None:
        logger.debug(f"Sending prompt to GPT: {text!r}")

        if system_prompt is None:
            system_prompt = prompt_manager.get_current_prompt()

        gpt_config = {
            "temperature": 1,
            "max_tokens": 256,
            "top_p": 1,
            "frequency_penalty": 0,
            "presence_penalty": 0,
            "stop": ["\n"],
        }
        user_prompt = {"role": "user", "content": text}
        messages: list = system_prompt + context.all() + [user_prompt]

        response = self.completion(messages, gpt_config)()

        if add_to_context:
            context.put(user_prompt)

        if hypertranslate:
            if isinstance(response, Iterator):
                response = "".join(list(response))

            hyper_response = self.hyper_gpt(response)()

            if isinstance(hyper_response, Iterator):

                def wrap() -> Generator[str, None, None]:
                    nonlocal hyper_response
                    full_text = ""
                    for c in hyper_response:
                        full_text += c
                        yield c
                    if add_to_context:
                        context.put({"role": "assistant", "content": full_text})

                return wrap()

            elif isinstance(hyper_response, str):
                if add_to_context:
                    context.put({"role": "assistant", "content": hyper_response})

                return hyper_response

        return response

    def hyper_gpt(self, text: str | None) -> Callable[[], Generator[str, None, None]] | Callable[[], str | None]:
        system_prompt = """
        Seu trabalho é pegar o texto enviado pelo usuário e transformar em um texto sem muito sentido, como se estivesse traduzindo o texto para 300 línguas diferentes, e depois de volta para português, a resposta não deve ter sentido, gere coisas aleatórias.
        Você não deve retornar nada além do texto transformado.
        Você não deve traduzir nada.
        Apenas envie o texto transformado em coisas sem sentido.
        As frases transformadas não devem ter coesão.
        Você deve escrever coisas aleatórias.

        Exemplos: '''
        Hoje está um dia lindo lá fora. Que legal!
        R:Outro bom dia. O que isso significa?

        Felps quebrou areia com as mãos... duas vezes. Que esforço hercúleo! E para quê? Para pegar mais areia! Incrível como a vida é cheia de desafios emocionantes e recompensas... arenosas. Será que Felps vai construir um castelo de areia ou apenas admirar seu vasto suprimento de grãos minerais?
        R:Ao se sentar, ele enterrou as mãos na areia. Não perca! Venha sozinho, uma vida feliz é cheia de desafios e recompensas. A casa de Phelps é feita de areia ou pedra?

        Por outro lado, o novo modelo estrutural aqui preconizado possibilita uma melhor visão global das novas proposições.
        R:Este é considerado o novo lar legal.

        Ah, Felps, abraçando a noite escura como um velho amigo. Com um machado na mão, ele afasta monstros e lendas. Um caçador nasce, não do medo, mas da pura ironia. Bravo!
        R:Olá, este é meu velho amigo Philip do Cavaleiro das Trevas. Lute contra anjos e demônios com machados, não tema ninguém além dos pecadores.

        E assim, Feeeelps escolheu ignorar a ferramenta mais básica em Minecraft para quebrar um minério de diamante com as próprias mãos. Uma decisão audaciosa, sem dúvida. Pena que as mãos não são picaretas e o diamante permanecerá eternamente na rocha, aguardando alguém que entenda como jogar.
        R:Philip decide abrir uma mina de diamantes. Esta é uma má decisão. Mas os soldados não ouviram as nossas palavras.
        '''

        Não repita os exemplos.
        Não gere nada com "R:".
        Retorne apenas a frase transformada.
        """

        logger.debug(f"Sending prompt to Hyper-GPT: {text!r}")
        user_message = {"role": "user", "content": text}
        system_message = {"role": "system", "content": system_prompt}
        messages: list = [system_message] + [user_message]

        response = self.completion(
            messages,
            {
                "temperature": 1.2,
                "max_tokens": 256,
                "top_p": 1,
                "frequency_penalty": 0,
                "presence_penalty": 0,
                "stop": ["\n"],
            },
        )

        return response

    def set_config(self, config: GlobalConfig):
        self.client.api_key = config.openai_api_key
        self.client.base_url = config.openai_base_url
        self.model = config.openai_model


chat = ChatGPT(
    global_config.openai_api_key,
    global_config.openai_base_url,
    global_config.openai_model,
)
