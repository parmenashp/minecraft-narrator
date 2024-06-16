import json
import gradio as gr
from src.config import global_config
from src.prompts import prompt_manager
from src.websocket import ws
from loguru import logger


def change_prompt(prompt_id: str, voice_id: str, model: str, clear_context: bool):
    logger.info(f"Setting prompt to {prompt_id} and voice {voice_id}")
    if prompt_id not in list(prompt_manager.prompts):
        return f"Prompt {prompt_id} does not exist"
    global_config.elevenlabs_voice_id = voice_id
    global_config.elevenlabs_model = model
    prompt_manager.set_current_prompt(prompt_id, clear_context)
    r = f"Prompt setted to {prompt_id} with model {model} and voice {voice_id}"
    if clear_context:
        r += " and context cleared"
    return r


def save_prompt(prompt_id: str, prompt: str, interactions: str):
    logger.info(f"Saving prompt {prompt_id}")
    if interactions == "":
        interactions_list = []
    else:
        interactions_list = interactions.split(" ")
    prompt_manager.new_custom_prompt(prompt_id, prompt, interactions_list)


async def change_personality(personality_id: str, checkboxes: list):
    clear_context = "Clear context" in checkboxes
    notify_minecraft = "Notify Minecraft" in checkboxes
    fireworks = "Fireworks" in checkboxes
    logger.info(f"Setting personality to {personality_id}")

    if personality_id not in list(prompt_manager.personalities):
        return f"Personality {personality_id} does not exist"

    prompt_manager.set_personality(personality_id, clear_context)
    prompt_manager.set_current_prompt(prompt_manager.personalities[personality_id]["prompt_id"], clear_context)
    global_config.elevenlabs_voice_id = prompt_manager.personalities[personality_id]["voice_id"]
    global_config.elevenlabs_model = prompt_manager.personalities[personality_id]["model"]

    if notify_minecraft:
        data = json.dumps(prompt_manager.personalities[personality_id])
        await ws.broadcast({"action": "new_personality", "data": data})

    if fireworks:
        await ws.broadcast({"action": "fireworks"})

    return f"Personality setted to {personality_id}"


def new_personality(personality_id: str, prompt_id: str, model: str, voice_id: str):
    logger.info(f"Creating new personality {personality_id}")

    prompt_manager.personalities[personality_id] = {
        "prompt_id": prompt_id,
        "voice_id": voice_id,
        "model": model,
    }
    prompt_manager.save()
    return f"Personality {personality_id} created"


def config_tab():
    with gr.Tab("Config") as tab:
        with gr.Tab("Global Config"):
            gr.Markdown(
                value=lambda: global_config.as_markdown(),
                every=5,
            )

        with gr.Tab("Change Personality"):
            gr.Interface(
                fn=change_personality,
                inputs=[
                    gr.Textbox(
                        label="Personality",
                        value=lambda: prompt_manager.current_personality_id,
                    ),
                    gr.CheckboxGroup(
                        label="Clear context",
                        choices=["Clear context", "Notify Minecraft", "Fireworks"],
                        container=False,
                    ),
                ],
                outputs="text",
                allow_flagging="never",
            )

        with gr.Tab("Personalities"):

            def personalities_html():
                return "\n".join(
                    [
                        f"\n<details><summary>{key}</summary>\n<pre>\n{prompt_manager.personalities[key]}\n</pre>\n</details>"
                        for key in prompt_manager.personalities
                    ]
                )

            gr.HTML(
                value=lambda: personalities_html(),
                every=5,
            )

        with gr.Tab("New personality"):
            gr.Interface(
                fn=new_personality,
                inputs=[
                    gr.Textbox(
                        label="Personality name",
                        placeholder="personality_name",
                    ),
                    gr.Textbox(
                        label="Prompt ID",
                        placeholder="prompt0",
                    ),
                    gr.Dropdown(
                        label="Model ID",
                        choices=["eleven_multilingual_v2", "eleven_multilingual_v1"],
                    ),
                    gr.Textbox(
                        label="Voice ID",
                        placeholder="9Fa9ozDyMkNFPnyRbRZD",
                    ),
                ],
                outputs="text",
                allow_flagging="never",
            )

        with gr.Tab("Change Prompt"):
            gr.Interface(
                fn=change_prompt,
                inputs=[
                    gr.Textbox(
                        label="Prompt ID",
                        value=lambda: prompt_manager.current_prompt_id,
                    ),
                    gr.Textbox(
                        label="Voice ID",
                        value=lambda: global_config.elevenlabs_voice_id,
                    ),
                    gr.Dropdown(
                        label="Voice Model",
                        choices=["eleven_multilingual_v2", "eleven_multilingual_v1"],
                        value=lambda: global_config.elevenlabs_model,
                    ),
                    gr.Checkbox(label="Clear context", value=False),
                ],
                outputs="text",
                allow_flagging="never",
            )
            gr.Textbox(
                value=lambda: f"Current prompt: {prompt_manager.current_prompt_id}\nCurrent voice: {global_config.elevenlabs_voice_id}\nCurrent model: {global_config.elevenlabs_model}",
                interactive=False,
                every=1,
                lines=3,
                max_lines=3,
                container=False,
            )

        with gr.Tab("Prompts"):

            def prompts_html():
                return "\n".join(
                    [
                        f"\n<details><summary>{id}</summary>\n<pre>\n{prompt["system_message"]}\n {prompt['interactions']}\n</pre>\n</details>"
                        for id, prompt in prompt_manager.prompts.items()
                    ]
                )

            gr.HTML(lambda: prompts_html(), every=5)

        with gr.Tab(label="New Prompt"):
            custom_prompt_id = gr.Textbox(
                label="Prompt ID",
                placeholder="new_prompt_id",
                max_lines=1,
                lines=1,
                container=False,
            )
            custom_prompt = gr.Textbox(
                label="Prompt",
                placeholder="Enter prompt here",
                max_lines=10,
                lines=10,
                container=False,
            )
            custom_interactions = gr.Textbox(
                label="Interactions (separated by spaces)",
                placeholder="Separated by spaces: interaction1 interaction2 ...",
                max_lines=1,
                lines=1,
                container=False,
            )
            gr.Button(
                "Save Prompt",
            ).click(
                save_prompt,
                inputs=[custom_prompt_id, custom_prompt, custom_interactions],
            )
    return tab
