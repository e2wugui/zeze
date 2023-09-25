# noinspection PyUnresolvedReferences
import gen.demo as demo
from zeze.bean import EmptyBean


class ModuleModule1(demo.Module1.AbstractModule):
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

    # noinspection PyPep8Naming
    @staticmethod
    def getSpecialTypeIdFromBean(bean):
        return bean.type_id()

    # noinspection PyPep8Naming
    @staticmethod
    def createBeanFromSpecialTypeId(id):
        if id == 0:
            return EmptyBean()
        raise Exception("unknown type_id = " + id)

    def process_Protocol3(self, r):
        raise Exception("not implement for process_Protocol3")

    def process_Protocol4(self, r):
        raise Exception("not implement for process_Protocol4")

    def process_ProtocolNoProcedure(self, r):
        raise Exception("not implement for process_ProtocolNoProcedure")

    def process_ProtocolUseData(self, r):
        raise Exception("not implement for process_ProtocolUseData")

    def process_Rpc1_request(self, r):
        raise Exception("not implement for process_Rpc1_request")
