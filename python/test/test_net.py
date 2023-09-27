import unittest

from gen.demo.Module1 import BValue
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
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.connected = False

    def test_connect(self):
        client = ServiceClient().start_client("www.163.com", 80)
        while not client.future:
            Socket.select(1)

    def test_rpc_simple(self):
        server = Service("TestRpc.Server")
        first = FirstRpc()
        # print(first.type_id())
        server.add_protocol_handle(first.type_id(), "FirstRpc", FirstRpc, TestNet.process_FirstRpc_request)

        server.start_server("127.0.0.1", 5000)
        client = Client(self)
        client.add_protocol_handle(first.type_id(), "FirstRpc", FirstRpc)

        client.start_client("127.0.0.1", 5000).wait_for_connected()

        first = FirstRpc()
        first.arg.int_1 = 1234
        # print("SendFirstRpcRequest")

        first.send_for_wait(client.get_socket())
        # print("FirstRpc Wait End")
        self.assertEqual(first.arg.int_1, first.res.int_1)

    @staticmethod
    def process_FirstRpc_request(rpc):
        rpc.res.assign(rpc.arg)
        rpc.send_result()
        print("ProcessFirstRpcRequest result.Int1 = " + str(rpc.res.int_1))
