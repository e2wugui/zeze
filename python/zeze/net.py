import errno
import selectors
import socket


class Socket:
    selector = selectors.DefaultSelector()

    def __init__(self, service, s=None):
        assert isinstance(service, Service)
        self.service = service
        self.local_addr = ""
        self.local_port = 0
        self.remote_addr = ""
        self.remote_port = 0
        self.socket = s if s is not None else socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.setblocking(False)
        self.connected = False

    def listen(self, addr, port, backlog=100):
        self.local_addr = addr
        self.local_port = port
        self.socket.bind((addr, port))
        self.socket.listen(backlog)
        Socket.selector.register(self.socket, selectors.EVENT_READ, self)

    def connect(self, addr, port, local_addr="", local_port=0):
        assert port > 0
        self.local_addr = local_addr
        self.local_port = local_port
        self.remote_addr = addr
        self.remote_port = port
        if local_addr != "" or local_port != 0:
            self.socket.bind((local_addr, local_port))
        r = self.socket.connect_ex((addr, port))
        if r == 0:
            self.service.on_connected(self)
        elif r == errno.EWOULDBLOCK or r == errno.EINPROGRESS:
            Socket.selector.register(self.socket, selectors.EVENT_READ, self)
        else:
            self.service.on_connect_failed(self)

    def send(self, data):
        # TODO soc.socket.send(data)
        pass

    def close(self):
        Socket.selector.unregister(self.socket)
        self.socket.close()
        self.connected = False
        self.service.on_closed(self)

    def on_read(self):
        if self.connected:
            self.service.on_received(self)
        elif self.remote_port == 0:
            s, addr_port = self.socket.accept()
            soc = Socket(self.service, s)
            soc.local_addr = self.local_addr
            soc.local_port = self.local_port
            soc.remote_addr = addr_port[1]
            soc.remote_port = addr_port[2]
            if self.service.on_accepted(soc):
                soc.connected = True
                Socket.selector.register(s, selectors.EVENT_READ, self)
            else:
                soc.socket.close()
        else:
            self.service.on_connected(self)

    def on_write(self):
        # TODO
        pass

    @staticmethod
    def select(timeout):
        for key, mask in Socket.selector.select(timeout):
            soc = key.data
            if mask & selectors.EVENT_READ:
                soc.on_read()
            if mask & selectors.EVENT_WRITE:
                soc.on_write()


class Service:
    # noinspection PyMethodMayBeStatic,PyUnusedLocal
    def on_accepted(self, soc):
        return True

    def on_connected(self, soc):
        pass

    def on_connect_failed(self, soc):
        pass

    def on_closed(self, soc):
        pass

    def on_received(self, soc):
        # TODO: soc.socket.recv(1000)
        pass


class Protocol:
    def __init__(self):
        # TODO
        pass


class Rpc:
    def __init__(self):
        # TODO
        pass
