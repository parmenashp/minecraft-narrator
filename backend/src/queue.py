import re
from typing import Generic, TypeVar

T = TypeVar("T")


class Queue(Generic[T]):
    def __init__(self, maxsize=4, join_duplicates=False):
        self._maxsize = maxsize
        self._queue: list[T] = []
        self.join_duplicates = join_duplicates

    def put(self, item: T):
        if len(self._queue) >= self._maxsize:
            self._queue.pop(0)
        if self.join_duplicates:
            self._queue = self.append_count_to_string(self._queue, item)
        else:
            self._queue.append(item)

    def all(self):
        return self._queue

    def get(self):
        return self._queue.pop(0)

    def clear(self):
        self._queue.clear()

    @staticmethod
    def append_count_to_string(lst, s):
        # Pattern to match the string and the count at the end if it exists
        pattern = re.escape(s) + r"( \d+ vezes)?"

        # Find all occurrences of the string in the list
        matches = [match for match in lst if re.fullmatch(pattern, match)]

        # Determine the new count
        if matches:
            # Find the highest count
            highest_count = 1
            for match in matches:
                count_match = re.search(r"(\d+) vezes", match)
                if count_match:
                    count = int(count_match.group(1))
                    highest_count = max(highest_count, count)

            # Increment the count
            new_count = highest_count + 1
            new_string = f"{s} {new_count} vezes"
        else:
            # If the string is not in the list, just use it as is
            new_string = s

        # Replace the last occurrence in the list or append the new string
        if matches:
            last_occurrence_index = max(lst.index(match) for match in matches)
            lst[last_occurrence_index] = new_string
        else:
            lst.append(new_string)

        return lst
