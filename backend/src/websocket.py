import asyncio

from fastapi import WebSocket
from loguru import logger


class ConnectionManager:
    def __init__(self):
        self.active_connections: list[WebSocket] = []
        self.event_loop: asyncio.AbstractEventLoop = asyncio.get_event_loop()

    async def connect(self, websocket: WebSocket):
        logger.info("Accepting new connection")
        await websocket.accept()
        self.active_connections.append(websocket)
        logger.info("New connection accepted and added to active connections")

    def disconnect(self, websocket: WebSocket):
        logger.info("Removing connection from active connections")
        self.active_connections.remove(websocket)

    async def broadcast(self, data: dict):
        logger.info(f"Broadcasting data: {data!r}")
        for connection in self.active_connections:
            await connection.send_json(data)

    def sync_broadcast(self, data: dict):
        asyncio.run_coroutine_threadsafe(self.broadcast(data), self.event_loop)


ws = ConnectionManager()
