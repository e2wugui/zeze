# noinspection PyUnresolvedReferences
import gen.demo as demo


class ModuleM6(demo.M6.AbstractModule):
    def __init__(self, app):
        super().__init__(app)

    def init(self):
        pass

    def start(self):
        pass

    def stop(self):
        pass

    def start_last(self):
        pass
