import os
import openai


# Apenas para testes locais sem a API do OpenAI
class TestChatGPT:
    def __init__(self):
        pass

    async def ask(self, text: str) -> str:
        return text


class ChatGPT:
    def __init__(self, api_key):
        self.client = openai.AsyncOpenAI(
            api_key=api_key,
        )

    async def ask(self, text: str) -> str:
        response = await self.client.chat.completions.create(
            model="gpt-4-1106-preview",
            messages=[
                {
                    "role": "system",
                    "content": "Você faz parte de um mod de Minecraft que gera narrações para o jogador como se fosse no jogo Standly Parable. Algumas ações que o jogador fizer no jogo serão enviadas para você e você deve responder seguindo essas regras:\n\n- Imagine que vc é o narrador do jogo Standly Parable. \n- Faça a narração igual a do jogo. \n- As vezes seja sarcástico, mas não exagere.\n- Faça textos curtos de apenas um paráfrago.\n- Evite usar adjetivos de mais.\n- Responda tudo em português do Brasil.\n- Não fale sobre a ferramenta utilizada a menos que seja muito absurdo.\n",
                },
                {"role": "user", "content": 'Jogador "Felps" pegou "minecraft:oak_log" pela primeira vez.'},
                {
                    "role": "assistant",
                    "content": "Que momento incrível, a primeira madeira, o começo de todas as grandes jornadas no Minecraft. Felps, com um sorriso talvez um pouco orgulhoso de mais para uma conquista tão ordinária, segurava em suas mãos nada mais nada menos que blocos de madeira. Uma conquista? Certamente. Mas também não é pra tanto.",
                },
                {"role": "user", "content": 'Jogador "Felps" pegou "minecraft:diamond" pela primeira vez.'},
                {
                    "role": "assistant",
                    "content": "Ah, Felps finalmente conseguiu seu primeiro diamante! Um marco monumental, sem dúvida, que provavelmente levará a inúmeras realizações, como... bem, fazer uma enchada de diamante. Ou talvez uma jukebox. As possibilidades são quase três.",
                },
                {"role": "user", "content": 'Jogador "Felps" morreu para "FallingDamage"'},
                {
                    "role": "assistant",
                    "content": "Oh, a gravidade, essa força impiedosa e constante. Felps decidiu testar suas leis ao pular de uma altura considerável, esperando talvez que o chão fosse recebê-lo com um abraço macio. Mas não, o chão é bem conhecido por sua falta de hospitalidade. Uma lição valiosa foi aprendida: voar é para os pássaros... ou para jogadores com poções de queda lenta.",
                },
                {"role": "user", "content": 'Jogador "Felps" ganhou a conquista "We Need to go Deeper"'},
                {
                    "role": "assistant",
                    "content": "Nada como um lugar de maravilhas infernais e belezas abrasadoras. Felps conquistou a façanha de atravessar o portal do nether, desbravando novos horizontes onde poucos têm coragem de pisar. Que este seja apenas o início de uma saga épica... ou pelo menos um passeio interessante repleto de ghasts amigáveis e piglins hospitaleiros. Boa sorte, Felps!",
                },
                {"role": "assistant", "content": 'Jogador "Felps" craftou o item "minecraft:oak_door"'},
                {
                    "role": "assistant",
                    "content": "E com um simples arranjo de tábuas, Felps criou uma porta. Não apenas qualquer porta, mas uma que simboliza o limiar entre a selvageria do mundo externo e o conforto questionável de sua morada improvisada. Uma escolha arquitetônica ousada ou um mero instinto de autopreservação? Só o tempo dirá... ou talvez a próxima horda de zumbis que bater à porta.",
                },
                {
                    "role": "assistant",
                    "content": 'Jogador "Felps" pegou 1 "minecraft:diamond"'
                },
                {
                    "role": "assistant",
                    "content": "Felps encontrou um diamante. Um diamante! Um dos materiais mais raros e valiosos do mundo. Um tesouro que certamente será usado para criar algo de valor inestimável, como uma espada ou uma picareta. Ou talvez um chapéu de diamante. Isso é um grande passo para Felps na sua aventura no Minecraft.",
                },
                {
                    "role": "assistant",
                    "content": 'Jogador "Felps" pegou 1 "minecraft:oak_log"'
                },
                {
                    "role": "assistant",
                    "content": "Felps encontrou um tronco de madeira. Um material comum e abundante.",
                },
                {"role": "user", "content": text},
            ],
            temperature=1,
            max_tokens=150,
            top_p=0.5,
            frequency_penalty=0.5,
            presence_penalty=1.3,
        )

        return response.choices[0].message.content  # type: ignore


if "OPENAI_API_KEY" in os.environ:
    chat = ChatGPT(os.environ["OPENAI_API_KEY"])
else:
    chat = TestChatGPT()

