import gradio as gr
from src.context import context

def get_context_as_chatbot() -> list[tuple[str, str]]:
    full_messages = []
    question_response = []
    for entries in context.all():
        question_response.append(entries["content"])
        if len(question_response) >= 2:
            full_messages.append(question_response)
            question_response = []
    if len(context.all()) % 2 != 0:
        # Make sure all messages are pairs
        full_messages.append(question_response)
        full_messages[-1].append(" ")
    return full_messages

def context_tab():
    with gr.Tab("Context") as tab:
        gr.Chatbot(
            value=get_context_as_chatbot,
            every=1,
            container=False,
            height=700,
            layout="panel",
            show_copy_button=True,
        )
    return tab
