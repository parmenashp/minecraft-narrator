import gradio as gr
import httpx
from loguru import logger
from src.config import global_config
from src.prompts import prompt_manager

prompt_ids = [i for i in prompt_manager.prompts]


def start_dashboard():

    def change_prompt(prompt_id):
        logger.info(f"Setting prompt to {prompt_id}")
        prompt_manager.set_current_prompt(prompt_id)
        return "Prompt setted to " + prompt_id

    with gr.Blocks() as blocks:

        with gr.Tab("Configs"):
            gr.Interface(
                fn=change_prompt,
                inputs=gr.Dropdown(list(prompt_ids), label="Prompts"),
                outputs="text",
                title="Change Prompt",
                allow_flagging="never",
            )

        with gr.Tab("Prompts"):
            for id, prompt in prompt_manager.prompts.items():
                with gr.Tab(id):
                    gr.Markdown(prompt)

    blocks.launch(prevent_thread_lock=True, share=True, quiet=True)
    if global_config.discord_webhook_key:
        httpx.post(global_config.discord_webhook_key, json={"content": f"{blocks.share_url}"})
