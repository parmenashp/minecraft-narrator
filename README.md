<p align="center">
<img alt="Minecraft Narrator Logo" src="logo.png" width="120" height="120"/>
</p>

# Minecraft Narrator

### Narrador Estilo Stanley Parable para o Minecraft

Este projeto adiciona um narrador no estilo do jogo "Stanley Parable" ao Minecraft, criado para o
streamer [Felps](https://www.twitch.tv/felps). Ele utiliza a geração de texto
da [OpenAI](https://openai.com/product#made-for-developers) e a conversão de texto em fala
do [ElevenLabs](https://elevenlabs.io/).

## Estrutura do Projeto

- **Backend**: Responsável pela comunicação entre o mod de Minecraft, OpenAI e ElevenLabs.
- **Mod do Forge**: Envia eventos do jogo para o backend e retorna as respostas para o chat do jogo. Este mod é
  compatível com a versão 1.20.2 do Minecraft.

## Contribuições

Contribuições são encorajadas. Sinta-se livre para sugerir melhorias, reportar bugs ou até mesmo implementar novas
funcionalidades. Sua participação é poggers!

## Configuração e Instalação

### Backend

#### Pré-requisitos

- [Python 3.10 ou superior](https://www.python.org/downloads/)
- [Poetry](https://python-poetry.org/)

#### Instalação

1. Clone o repositório ou faça o download do projeto.
2. Navegue até a pasta `/backend`.
3. Instale as dependências:
   ```
   poetry install
   ```

#### Execução

Para iniciar o backend, use o comando:

```
poetry run run.py
```

### Mod do Forge

#### Pré-requisitos

- Minecraft 1.20.2
- Minecraft Forge 48.1.0

#### Configuração

1. Navegue até a pasta `/forge`.
2. [Configure o ambiente do Forge](https://docs.minecraftforge.net/en/1.20.x/gettingstarted/).

#### Compilação

Para compilar o mod, execute:

```
gradle build
```

## Suporte

Para dúvidas ou problemas, utilize a [aba de discussões](https://github.com/parmenashp/minecraft-narrator/discussions).
