class Queue:
    def __init__(self, maxsize=4):
        self._maxsize = maxsize
        self._queue = []

    def put(self, item):
        if len(self._queue) >= self._maxsize:
            self._queue.pop(0)

        self._queue.append(item)

    def all(self):
        return self._queue

    def clear(self):
        self._queue.clear()
