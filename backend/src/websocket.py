import asyncio
from fastapi import WebSocket


class ConnectionManager:
    def __init__(self):
        self.active_connections: list[WebSocket] = []
        self.event_loop: asyncio.AbstractEventLoop = asyncio.get_event_loop()

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)

    def disconnect(self, websocket: WebSocket):
        self.active_connections.remove(websocket)

    async def broadcast(self, data: dict):
        for connection in self.active_connections:
            await connection.send_json(data)
        print("broadcasted:", data)

    def sync_broadcast(self, data: dict):
        asyncio.run_coroutine_threadsafe(self.broadcast(data), self.event_loop)


ws = ConnectionManager()
