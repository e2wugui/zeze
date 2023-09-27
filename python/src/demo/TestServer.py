# noinspection PyUnresolvedReferences
from zeze.net import *


class TestServer(Service):
    def __init__(self, app):
        super().__init__()
        self.app = app

    def start(self):
        pass  # TODO

    def stop(self):
        pass  # TODO
