from zeze.app import *
# noinspection PyUnresolvedReferences
import src.demo as demo


class App:
    instance = None

    @staticmethod
    def get_instance():
        return App.instance

    def start(self):
        self.create_zeze()
        self.create_service()
        self.create_modules()
        self.zeze.start()  # 启动数据库
        self.start_modules()  # 启动模块，装载配置什么的。
        self.start_service()  # 启动网络

    def stop(self):
        self.stop_service()  # 关闭网络
        self.stop_modules()  # 关闭模块，卸载配置什么的。
        self.zeze.stop()  # 关闭数据库
        self.destroy_modules()
        self.destroy_services()
        self.destroy_zeze()

    # // ZEZE_FILE_CHUNK {{{ GEN APP @formatter:off
    def __init__(self):
        self.zeze = None
        self.modules = {}

        self.TestServer = None

        self.demo_Module1 = None
        self.demo_Module1_Module11 = None
        self.demo_M6 = None
        self.demo_M6_M7 = None

    def get_zeze(self):
        return self.zeze

    def create_zeze(self, config=None):
        if self.zeze is not None:
            raise Exception("zeze has created!")

        self.zeze = Application("test", config)

    def create_service(self):
        self.TestServer = demo.TestServer(self.zeze)

    def create_modules(self):
        self.zeze.init(self)

        self.demo_Module1 = demo.Module1.ModuleModule1(self.zeze)
        self.demo_Module1.init()
        if self.demo_Module1.get_full_name() in self.modules:
            raise Exception("duplicate module name: demo_Module1")
        self.modules[self.demo_Module1.get_full_name()] = self.demo_Module1

        self.demo_Module1_Module11 = demo.Module1.Module11.ModuleModule11(self.zeze)
        self.demo_Module1_Module11.init()
        if self.demo_Module1_Module11.get_full_name() in self.modules:
            raise Exception("duplicate module name: demo_Module1_Module11")
        self.modules[self.demo_Module1_Module11.get_full_name()] = self.demo_Module1_Module11

        self.demo_M6 = demo.M6.ModuleM6(self.zeze)
        self.demo_M6.init()
        if self.demo_M6.get_full_name() in self.modules:
            raise Exception("duplicate module name: demo_M6")
        self.modules[self.demo_M6.get_full_name()] = self.demo_M6

        self.demo_M6_M7 = demo.M6.M7.ModuleM7(self.zeze)
        self.demo_M6_M7.init()
        if self.demo_M6_M7.get_full_name() in self.modules:
            raise Exception("duplicate module name: demo_M6_M7")
        self.modules[self.demo_M6_M7.get_full_name()] = self.demo_M6_M7

    def destroy_modules(self):
        self.demo_M6_M7 = None
        self.demo_M6 = None
        self.demo_Module1_Module11 = None
        self.demo_Module1 = None
        self.modules.clear()

    def destroy_services(self):
        self.TestServer = None

    def destroy_zeze(self):
        self.zeze = None

    def start_modules(self):
        self.demo_Module1.start()
        self.demo_Module1_Module11.start()
        self.demo_M6.start()
        self.demo_M6_M7.start()

    def start_last_modules(self):
        self.demo_Module1.start_last()
        self.demo_Module1_Module11.start_last()
        self.demo_M6.start_last()
        self.demo_M6_M7.start_last()

    def stop_modules(self):
        if self.demo_M6_M7 is not None:
            self.demo_M6_M7.stop()
        if self.demo_M6 is not None:
            self.demo_M6.stop()
        if self.demo_Module1_Module11 is not None:
            self.demo_Module1_Module11.stop()
        if self.demo_Module1 is not None:
            self.demo_Module1.stop()

    def stop_before_modules(self):
        if self.demo_M6_M7 is not None:
            self.demo_M6_M7.stop_before()
        if self.demo_M6 is not None:
            self.demo_M6.stop_before()
        if self.demo_Module1_Module11 is not None:
            self.demo_Module1_Module11.stop_before()
        if self.demo_Module1 is not None:
            self.demo_Module1.stop_before()

    def start_service(self):
        self.TestServer.start()

    def stop_service(self):
        if self.TestServer is not None:
            self.TestServer.stop()
    # // ZEZE_FILE_CHUNK }}} GEN APP @formatter:on
