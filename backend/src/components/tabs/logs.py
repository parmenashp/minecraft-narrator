import gradio as gr
from io import StringIO

dashboard_sink = StringIO()

def logs_tab():
    with gr.Tab("Logs") as tab:
        gr.Code(
            value=lambda: "\n".join(dashboard_sink.getvalue().splitlines()[-200:]),  # type: ignore
            label="Logs",
            interactive=False,
            every=1,
            language="typescript",
        )
    return tab
