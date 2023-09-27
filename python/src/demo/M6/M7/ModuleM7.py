# noinspection PyUnresolvedReferences
import gen.demo as demo


class ModuleM7(demo.M6.M7.AbstractModule):
    def __init__(self, app):
        super().__init__(app)

    def init(self):
        pass

    def start(self):
        pass

    def start_last(self):
        pass

    def stop_before(self):
        pass

    def stop(self):
        pass
