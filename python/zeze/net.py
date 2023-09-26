import errno
import selectors
import socket
import time

from zeze.bean import Serializable
from zeze.buffer import ByteBuffer


class Socket:
    selector = selectors.DefaultSelector()
    session_id_gen = 0

    def __init__(self, service, s=None):
        assert isinstance(service, Service)
        self.service = service
        self.local_addr = ""
        self.local_port = 0
        self.remote_addr = ""
        self.remote_port = 0
        self.socket = s if s is not None else socket.socket()
        self.socket.setblocking(False)
        self.connected = False
        self.recv_buf = ByteBuffer()
        self.send_buf = ByteBuffer()
        Socket.session_id_gen += 1
        self.session_id = Socket.session_id_gen

    def __str__(self):
        return f"[{self.session_id}] {self.local_addr}:{self.local_port}-{self.remote_addr}:{self.remote_port}"

    def listen(self, addr, port, backlog=100):
        # addr = socket.gethostbyname(addr)
        self.local_addr = addr
        self.local_port = port
        self.socket.bind((addr, port))
        self.socket.listen(backlog)
        Socket.selector.register(self.socket, selectors.EVENT_READ, self)

    def connect(self, addr, port, local_addr="", local_port=0):
        assert port > 0
        # addr = socket.gethostbyname(addr)
        self.local_addr = local_addr
        self.local_port = local_port
        self.remote_addr = addr
        self.remote_port = port
        if local_addr != "" or local_port != 0:
            self.socket.bind((local_addr, local_port))
        Socket.selector.register(self.socket, selectors.EVENT_READ | selectors.EVENT_WRITE, self)
        r = self.socket.connect_ex((addr, port))
        if r == 0:
            self.service.on_connected(self)
        elif r != errno.EWOULDBLOCK and r != errno.EINPROGRESS:
            self.service.on_connect_failed(self)

    def send(self, data):
        if not self.connected:
            return False
        if isinstance(data, Protocol):
            data = data.encode()
        writing = self.send_buf.size() > 0
        self.send_buf.append(data)
        if not writing:
            Socket.selector.modify(self.socket, selectors.EVENT_READ | selectors.EVENT_WRITE, self)
        return True

    def close(self):
        Socket.selector.unregister(self.socket)
        self.socket.close()
        self.connected = False
        self.service.on_closed(self)

    def on_read(self):
        if not self.connected:
            if self.remote_port == 0:
                s, addr_port = self.socket.accept()
                soc = Socket(self.service, s)
                soc.local_addr = self.local_addr
                soc.local_port = self.local_port
                soc.remote_addr = addr_port[0]
                soc.remote_port = addr_port[1]
                if self.service.on_accepted(soc):
                    soc.connected = True
                    Socket.selector.register(s, selectors.EVENT_READ, soc)
                else:
                    soc.socket.close()
                return
            self.connected = True
            self.service.on_connected(self)
        r = self.socket.recv(0x100000)
        if len(r) > 0:
            self.recv_buf.append(r)
            self.service.on_received(self)

    def on_write(self):
        if not self.connected:
            self.connected = True
            self.service.on_connected(self)
        buf = self.send_buf
        while True:
            n = min(buf.size(), 0x100000)
            if n <= 0:
                break
            r = self.socket.send(buf.buf[buf.ri:buf.ri + n])
            if r > 0:
                buf.ri += r
            if r < n:
                break
        if buf.size() > 0:
            buf.compact()
        else:
            Socket.selector.modify(self.socket, selectors.EVENT_READ, self)

    @staticmethod
    def select(timeout):
        for key, mask in Socket.selector.select(timeout):
            soc = key.data
            if mask & selectors.EVENT_READ:
                soc.on_read()
            if mask & selectors.EVENT_WRITE:
                soc.on_write()
        Rpc.check_timeout()


class ProtocolHandle:
    def __init__(self, name, t, handle):
        self.name = name  # str
        self.type = t  # type
        self.handle = handle  # method


class Service:
    def __init__(self, name):
        self.name = name
        self.opt_max_protocol_size = 2 * 1024 * 1024
        self.protocol_handles = {}  # type_id => ProtocolHandle

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
        self.decode(soc, soc.recv_buf)

    def on_rpc_lost_context(self, rpc):
        pass

    def add_protocol_handle(self, type_id, name, t, handle=None):
        self.protocol_handles[type_id] = ProtocolHandle(name, t, handle)

    def remove_protocol_handle(self, type_id):
        del self.protocol_handles[type_id]

    @staticmethod
    def make_type_id(module_id, protocol_id):
        return (module_id << 32) + (protocol_id & 0xffff_ffff)

    def decode(self, soc, bb):
        while bb.size() >= Protocol.HEADER_SIZE:
            buf = bb.buf
            begin_ri = bb.ri
            module_id = ByteBuffer.to_int(buf, begin_ri)
            protocol_id = ByteBuffer.to_int(buf, begin_ri + 4)
            size = ByteBuffer.to_int(buf, begin_ri + 8)
            if Protocol.HEADER_SIZE + size > bb.size():
                max_size = self.opt_max_protocol_size
                if size > max_size:
                    protocol_handle = self.protocol_handles.get(Service.make_type_id(module_id, protocol_id))
                    pname = protocol_handle.name if protocol_handle is not None else "?"
                    raise Exception(f"protocol '{pname}' in '{self.name}' module={module_id} protocol={protocol_id}"
                                    f" size={size}>{max_size} too large!")
                return
            begin_ri += Protocol.HEADER_SIZE
            bb.ri = begin_ri
            end_ri = begin_ri + size
            saved_wi = bb.wi
            bb.wi = end_ri

            if self.check_throttle(soc, module_id, protocol_id, size):
                type_id = Service.make_type_id(module_id, protocol_id)
                protocol_handle = self.protocol_handles.get(type_id)
                if protocol_handle is not None:
                    self.dispatch_protocol(type_id, bb, protocol_handle, soc)
                else:
                    self.dispatch_unknown_protocol(soc, module_id, protocol_id, bb)
            bb.ri = end_ri
            bb.wi = saved_wi
        bb.compact()

    # noinspection PyMethodMayBeStatic,PyUnusedLocal
    def check_throttle(self, soc, module_id, protocol_id, size):
        return True

    # noinspection PyUnusedLocal
    def dispatch_protocol(self, type_id, bb, protocol_handle, soc):
        p = protocol_handle.type()
        p.decode(bb)
        p.sender = soc
        p.handle(self, protocol_handle)

    def dispatch_unknown_protocol(self, soc, module_id, protocol_id, bb):
        raise Exception(f"unknown protocol({module_id}, {protocol_id}) size={bb.size()} for {soc}")


class Protocol(Serializable):
    HEADER_SIZE = 12  # module_id[4] + protocol_id[4] + size[4]

    Protocol = 2
    Request = 1
    Response = 0
    BitResultCode = 1 << 5
    FamilyClassMask = BitResultCode - 1

    def __init__(self):
        self.arg = None  # Bean
        self.result_code = 0
        self.sender = None  # Socket
        self.user_state = None  # any

    def __str__(self):
        return f"{self.__class__.__name__} result_code={self.result_code} arg={self.arg}"

    def get_module_id(self):
        raise NotImplementedError("Protocol.get_module_id")

    def get_protocol_id(self):
        raise NotImplementedError("Protocol.get_protocol_id")

    def type_id(self):
        return Service.make_type_id(self.get_module_id(), self.get_protocol_id())

    def get_pre_alloc_size(self):
        return 10 + self.arg.get_pre_alloc_size()  # [1]family_class + [9]result_code

    def set_pre_alloc_size(self, size):
        self.arg.set_pre_alloc_size(size - 1)  # [1]family_class

    def encode(self, bb=None):
        if bb is None:
            pre_alloc_size = self.get_pre_alloc_size()
            bb = ByteBuffer(min(Protocol.HEADER_SIZE + pre_alloc_size, 0x10000))
            bb.write_int4(self.get_module_id())
            bb.write_int4(self.get_protocol_id())
            save_size = bb.begin_write_with_size4()
            self.encode(bb)
            bb.end_write_with_size4(save_size)
            size = bb.size() - save_size - 4
            if size > self.get_pre_alloc_size():
                self.set_pre_alloc_size(size)
        else:
            if self.result_code == 0:
                bb.write_int(Protocol.Protocol)
            else:
                bb.write_int(Protocol.Protocol | Protocol.BitResultCode)
                bb.write_long(self.result_code)
            self.arg.encode(bb)
        return bb

    def decode(self, bb):
        header = bb.read_int()
        if (header & Protocol.FamilyClassMask) != Protocol.Protocol:
            raise Exception(f"invalid header({header}) for decoding protocol {self.__class__.__name__}")
        self.result_code = bb.read_long() if header & Protocol.BitResultCode else 0
        self.arg.decode(bb)

    def handle(self, service, protocol_handle):
        handle = protocol_handle.handle
        if handle is not None:
            handle(self)
        else:
            print(f"handle({service.name}): protocol handle not found: {self}")

    def send(self, soc):
        if soc is None:
            return False
        self.sender = soc
        return soc.send(self)

    def send_result(self, code=None):
        pass

    def try_send_result(self, code=None):
        pass


class Rpc(Protocol):
    session_id_gen = 0
    next_check_time = 0
    contexts = {}  # session_id => rpc

    def __init__(self):
        super().__init__()
        self.res = None  # Bean
        self.session_id = 0
        self.response_handle = None
        self.timeout = 0
        self.is_timeout = False
        self.is_request = True
        self.send_result_done = False

    def __str__(self):
        return (f"{self.__class__.__name__} is_request={self.is_request} session_id={self.session_id}"
                f" result_code={self.result_code} arg={self.arg} res={self.res}")

    @staticmethod
    def check_timeout():
        cur_time = time.time()
        if cur_time < Rpc.next_check_time:
            return
        Rpc.next_check_time = cur_time + 1
        for sid, rpc in Rpc.contexts.items():
            if rpc.timeout <= cur_time:
                del Rpc.contexts[sid]
                rpc.is_timeout = True
                rpc.is_request = False
                rpc.result_code = -10  # timeout
                handle = rpc.response_handle
                if handle is not None:
                    handle(rpc)

    def encode(self, bb=None):
        if bb is None:
            return super().encode(bb)
        header = Protocol.Request if self.is_request else Protocol.Response
        if self.result_code == 0:
            bb.write_int(header)
        else:
            bb.write_int(header | Protocol.BitResultCode)
            bb.write_long(self.result_code)
        bb.write_long(self.session_id)
        if self.is_request:
            self.arg.encode(bb)
        else:
            self.res.encode(bb)
        return bb

    def decode(self, bb):
        header = bb.read_int()
        family_class = header & Protocol.FamilyClassMask
        if family_class is Protocol.Protocol:
            raise Exception(f"invalid header({header}) for decoding rpc {self.__class__.__name__}")
        self.is_request = family_class == Protocol.Request
        self.result_code = bb.read_long() if header & Protocol.BitResultCode else 0
        self.session_id = bb.read_long()
        if self.is_request:
            self.arg.decode(bb)
        else:
            self.res.decode(bb)

    def handle(self, service, protocol_handle):
        if self.is_request:
            super().handle(service, protocol_handle)
            return
        if self.session_id not in Rpc.contexts:
            service.on_rpc_lost_context(self)
            return
        context = Rpc.contexts.pop(self.session_id)
        context.result_code = self.result_code
        context.sender = self.sender
        context.res = self.res
        context.is_timeout = False
        context.is_request = False
        if context.response_handle is not None:
            context.response_handle(context)

    def send(self, soc, handle=None, timeout=5000):
        if soc is None:
            return False
        if Rpc.contexts.get(self.session_id) is self:
            del Rpc.contexts[self.session_id]
        sid = Rpc.session_id_gen
        while True:
            sid += 1
            if sid not in Rpc.contexts:
                break
        Rpc.session_id_gen = sid
        self.session_id = sid
        self.response_handle = handle
        self.timeout = time.time() + timeout
        self.is_timeout = False
        self.is_request = True
        Rpc.contexts[sid] = self
        if super().send(soc):
            return True
        del Rpc.contexts[sid]
        return False

    def send_result(self, code=None):
        if self.send_result_done:
            print(f"Rpc.send_result already done: {self.sender} {self}")
            return False
        self.send_result_done = True
        self.is_request = False
        if code is not None:
            self.result_code = code
        return super().send(self.sender)

    def try_send_result(self, code=None):
        if self.send_result_done:
            return False
        self.send_result_done = True
        self.is_request = False
        if code is not None:
            self.result_code = code
        return super().send(self.sender)
