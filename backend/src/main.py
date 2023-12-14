from flask import Flask, request, jsonify

app = Flask(__name__)


@app.route("/event", methods=["POST"])
def handle_event():
    if not request.is_json:
        return jsonify(error="Formato inválido"), 400

    event_data = request.get_json()

    if "event" not in event_data:
        return jsonify(error="Campo 'event' não encontrado"), 400

    event = event_data["event"]

    def handle_item_craft(event_detail):
        item_id = event_detail.get("item")
        if item_id:
            return jsonify(action="chat", text=f"Item com ID {item_id} foi criado")
        else:
            return jsonify(error="ID do item não fornecido"), 400

    def handle_block_break(event_detail):
        block_id = event_detail.get("block")
        if block_id:
            return jsonify(action="chat", text=f"Bloco com ID {block_id} foi quebrado")
        else:
            return jsonify(error="ID do bloco não fornecido"), 400

    event_handlers = {
        "item.craft": handle_item_craft,
        "block.break": handle_block_break,
    }

    handler = event_handlers.get(event["name"])
    if handler:
        return handler(event)
    else:
        return jsonify(error="Evento desconhecido"), 400


if __name__ == "__main__":
    app.run(debug=True)
