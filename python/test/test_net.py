import unittest

from gen.demo.Module1 import BValue, Protocol4
from zeze.net import Service, Socket, Rpc


class ServiceClient(Service):
    def __init__(self):
        super().__init__("TestNet.ServiceClient")
        self.future = False

    def on_connected(self, soc):
        super().on_connected(soc)
        print("OnSocketConnected: " + str(soc.session_id))
        head = b"GET / HTTP/1.1\r\nHost: www.163.com\r\nAccept:*/*\r\n\r\n"
        if not soc.send(head):
            print("send fail")

    def on_received(self, soc):
        buf = soc.recv_buf
        print("input size = " + str(buf.size()))
        print(buf.copy().decode("utf-8"))
        buf.ri = buf.wi
        self.future = True


class FirstRpc(Rpc):
    def __init__(self):
        super().__init__()
        self.arg = BValue()
        self.res = BValue()

    def get_module_id(self):
        return 1

    def get_protocol_id(self):
        return -1


class Client(Service):
    def __init__(self, test):
        super().__init__("TestNet.Client")
        self.test = test

    def on_connected(self, soc):
        super().on_connected(soc)
        self.test.connected = True


class TestNet(unittest.TestCase):
    instance = None

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.connected = False
        self.handled = False
        TestNet.instance = self

    def test_connect(self):
        client = ServiceClient().start_client("www.163.com", 80)
        while not client.future:
            Socket.select(1)

    def test_rpc_simple(self):
        server = Service("TestRpc.Server")
        rpc = FirstRpc()
        server.add_protocol_handle(Protocol4.TypeId, "Protocol4", Protocol4, self.process_Protocol4)
        server.add_protocol_handle(rpc.type_id(), "FirstRpc", FirstRpc, TestNet.process_FirstRpc_request)

        server.start_server("127.0.0.1", 5000)
        client = Client(self)
        client.add_protocol_handle(rpc.type_id(), "FirstRpc", FirstRpc)

        client.start_client("127.0.0.1", 5000).wait_for_connected()

        Protocol4(BValue(1234)).send(client.get_socket())
        while not self.handled:
            Socket.select(1)

        rpc = FirstRpc()
        rpc.arg.int_1 = 4321
        rpc.send_for_wait(client.get_socket())
        self.assertEqual(rpc.arg.int_1, rpc.res.int_1)

    def process_Protocol4(self, p):
        print("process_Protocol4 arg.Int1 = " + str(p.arg.int_1))
        self.assertEqual(1234, p.arg.int_1)
        self.handled = True

    @staticmethod
    def process_FirstRpc_request(rpc):
        rpc.res.assign(rpc.arg)
        print("process_FirstRpc_request res.Int1 = " + str(rpc.res.int_1))
        TestNet.instance.assertEqual(4321, rpc.res.int_1)
        rpc.send_result()
