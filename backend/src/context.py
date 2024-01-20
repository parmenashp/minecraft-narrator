import json
import os
from src.queue import Queue


class Context:
    def __init__(self, maxsize=14):
        self.queue = Queue(maxsize=maxsize)
        if os.path.exists("data.json"):
            self.load()

    def put(self, item):
        self.queue.put(item)
        self.save()

    def all(self):
        return self.queue.all()

    def save(self):
        with open("data.json", "w") as f:
            json.dump(self.all(), f, indent=2, ensure_ascii=False)

    def load(self):
        try:
            with open("data.json", "r") as f:
                for i in json.load(f):
                    self.queue.put(i)
        except:
            pass
