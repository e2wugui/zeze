import struct

from zeze.vector import *
from zeze.util import *


class ByteBuffer:
    empty = bytearray(0)
    empty_bytes = bytes()

    def __init__(self, buffer=bytearray(16), read_index=0, write_index=0):
        if not isinstance(buffer, bytearray):
            if buffer == 0:
                buffer = ByteBuffer.empty
            else:
                buffer = bytearray(buffer)
        self.buf = buffer  # bytearray
        self.ri = read_index  # int
        self.wi = write_index  # int
        self.verify_index()

    def verify_index(self):
        ri = self.ri
        wi = self.wi
        rit = type(ri)
        wit = type(wi)
        if rit != int or wit != int:
            raise Exception(f"ri or wi is not int: {rit}, {wit}")
        buf_len = len(self.buf)
        if ri < 0 or wi < ri or buf_len < wi:
            raise Exception(f"ri or wi is invalid: {ri} <= {wi} <= {buf_len}")

    @staticmethod
    def verify_array_index(buffer, offset, length):
        if not isinstance(buffer, bytearray):
            raise Exception(f"buffer is not bytearray: {type(buffer)}")
        if type(offset) != int or type(length) != int:
            raise Exception(f"offset or length is not int: {type(offset)}, {type(length)}")
        buf_len = len(buffer)
        if offset < 0 or length < 0 or offset + length > buf_len:
            raise Exception(f"offset or length is invalid: {offset} + {length} > {buf_len}")

    def capacity(self):
        return len(self.buf)

    def size(self):
        return self.wi - self.ri

    def is_empty(self):
        return self.ri >= self.wi

    @staticmethod
    def wrap(bb_or_bytes, offset=0, length=-1):
        if isinstance(bb_or_bytes, ByteBuffer):
            offset = bb_or_bytes.ri
            length = bb_or_bytes.size()
            bb_or_bytes = bb_or_bytes.buf
        elif length < 0:
            length = len(bb_or_bytes)
        return ByteBuffer(bb_or_bytes, offset, offset + length)

    def free_internal_buffer(self):
        self.buf = ByteBuffer.empty
        self.reset()

    def wraps(self, bb_or_bytes, offset=0, length=-1):
        if isinstance(bb_or_bytes, ByteBuffer):
            offset = bb_or_bytes.ri
            length = bb_or_bytes.size()
            bb_or_bytes = bb_or_bytes.buf
        else:
            if length < 0:
                length = len(bb_or_bytes)
            bb_or_bytes = bytearray(bb_or_bytes)
        ByteBuffer.verify_array_index(bb_or_bytes, offset, length)
        self.buf = bb_or_bytes
        self.ri = offset
        self.wi = offset + length

    def append(self, buffer, offset=0, length=-1):
        if length < 0:
            length = len(buffer)
        self.ensure_write(length)
        wi = self.wi
        self.wi = wi + length
        self.buf[wi: wi + length] = buffer[offset: offset + length]

    def replace(self, write_index, buffer, offset=0, length=-1):
        if length < 0:
            length = len(buffer)
        if write_index < self.ri or write_index + length > self.wi:
            raise Exception(f"replace write_index={write_index}, length={length} at {self.ri}/{self.wi}")
        self.buf[write_index: write_index + length] = buffer[offset: offset + length]

    def begin_write_with_size4(self):
        save_size = self.size()
        self.ensure_write(4)
        self.wi += 4
        return save_size

    def end_write_with_size4(self, save_size):
        old_write_index = self.ri + save_size
        if old_write_index + 4 > self.wi:
            raise Exception(f"end_write_with_size4 save_size={save_size} at {self.ri}/{self.wi}")
        struct.pack_into("<i", self.buf, old_write_index, self.wi - old_write_index - 4)

    @staticmethod
    def to_int(buffer, offset):
        return struct.unpack_from("<i", buffer, offset)[0]

    @staticmethod
    def to_int_be(buffer, offset):
        return struct.unpack_from(">i", buffer, offset)[0]

    @staticmethod
    def to_long(buffer, offset):
        return struct.unpack_from("<q", buffer, offset)[0]

    @staticmethod
    def to_long_be(buffer, offset):
        return struct.unpack_from(">q", buffer, offset)[0]

    @staticmethod
    def to_float(buffer, offset):
        return struct.unpack_from("<f", buffer, offset)[0]

    @staticmethod
    def to_float_be(buffer, offset):
        return struct.unpack_from(">f", buffer, offset)[0]

    @staticmethod
    def to_double(buffer, offset):
        return struct.unpack_from("<d", buffer, offset)[0]

    @staticmethod
    def to_double_be(buffer, offset):
        return struct.unpack_from(">d", buffer, offset)[0]

    @staticmethod
    def to_long_n(buffer, offset, length):
        v = 0
        if length >= 8:
            return struct.unpack_from("<q", buffer, offset)[0]
        for i in range(0, length):
            v += buffer[offset + i] << (i * 8)
        return v

    @staticmethod
    def to_long_be_n(buffer, offset, length):
        v = 0
        if length >= 8:
            return struct.unpack_from(">q", buffer, offset)[0]
        for i in range(0, length):
            v = (v << 8) + buffer[offset + i]
        return v

    def compact(self):
        """
        这个方法把剩余可用数据移到buffer开头。
        【注意】这个方法会修改ReadIndex，WriteIndex。
        最好仅在全部读取写入处理完成以后调用处理一次，
        为下一次写入读取做准备。
        """
        size = self.size()
        if size > 0:
            if self.ri > 0:
                self.buf[0: size] = self.buf[self.ri: self.ri + size]
                self.ri = 0
                self.wi = size
        else:
            self.reset()

    def copy(self):
        size = self.size()
        if size == 0:
            return ByteBuffer.empty
        copy = bytearray(size)
        copy[0: size] = self.buf[self.ri: self.ri + size]
        return copy

    def copy_if(self):
        return self.buf if self.wi == len(self.buf) and self.ri == 0 else self.copy()

    def reset(self):
        self.ri = 0
        self.wi = 0

    def to_power2(self, need_size):
        if need_size < 0 or need_size > 0x4000_0000:
            raise Exception(f"invalid need_size={need_size} at {self.ri}/{self.wi}")
        size = 16
        while size < need_size:
            size <<= 1
        return size

    def grow_capacity(self, new_size):
        new_buf = bytearray(self.to_power2(new_size))
        ri = self.ri
        size = self.wi - ri
        new_buf[0: size] = self.buf[ri: ri + size]
        self.buf = new_buf
        self.ri = 0
        self.wi = size

    def ensure_write(self, size):
        new_size = self.wi + size
        if new_size > len(self.buf):
            self.grow_capacity(new_size)

    def raise_ensure_read_exception(self, size):
        raise Exception(f"ensure read {self.ri}+{size} > {self.wi}")

    def ensure_read(self, size):
        if self.ri + size > self.wi:
            self.raise_ensure_read_exception(size)

    def write_bool(self, b):
        self.ensure_write(1)
        wi = self.wi
        self.wi = wi + 1
        self.buf[wi] = b

    def read_bool(self):
        self.ensure_read(1)
        ri = self.ri
        self.ri = ri + 1
        b = self.buf[ri]
        if b == 1:  # fast-path
            return True
        if b == 0:  # fast-path
            return False
        return self.read_long() != 0  # rare-path

    def write_byte(self, v):
        self.ensure_write(1)
        wi = self.wi
        self.wi = wi + 1
        self.buf[wi] = v & 0xff

    def read_byte(self):
        self.ensure_read(1)
        ri = self.ri
        self.ri = ri + 1
        return self.buf[ri]

    def write_int4(self, v):
        self.ensure_write(4)
        wi = self.wi
        self.wi = wi + 4
        struct.pack_into("<i", self.buf, wi, v)

    def read_int4(self) -> int:
        self.ensure_read(4)
        ri = self.ri
        self.ri = ri + 4
        # noinspection PyTypeChecker
        return struct.unpack_from("<i", self.buf, ri)[0]

    def write_long8(self, v):
        self.ensure_write(8)
        wi = self.wi
        self.wi = wi + 8
        struct.pack_into("<q", self.buf, wi, v)

    def read_long8(self) -> int:
        self.ensure_read(8)
        ri = self.ri
        self.ri = ri + 8
        # noinspection PyTypeChecker
        return struct.unpack_from("<q", self.buf, ri)[0]

    @staticmethod
    def write_uint_size(v):
        # v看成无符号数时,与write_ulong_size的结果一致,即相当于write_ulong_size(v & 0xffff_ffff)
        # @formatter:off
        v &= 0xffff_ffff
        if v <        0x80: return 1
        if v <      0x4000: return 2
        if v <   0x20_0000: return 3
        if v < 0x1000_0000: return 4
        return 5
        # @formatter:on

    def write_uint(self, v):
        # v看成无符号数时,与write_ulong的结果一致,即相当于write_ulong(v & 0xffff_ffff)
        v &= 0xffff_ffff
        if v < 0x80:  # 0xxx xxxx
            self.ensure_write(1)
            wi = self.wi
            self.wi = wi + 1
            self.buf[wi] = v
        elif v < 0x4000:  # 10xx xxxx +1B
            self.ensure_write(2)
            wi = self.wi
            self.wi = wi + 2
            buf = self.buf
            buf[wi] = (v >> 8) + 0x80
            buf[wi + 1] = v & 0xff
        elif v < 0x20_0000:  # 110x xxxx +2B
            self.ensure_write(3)
            wi = self.wi
            self.wi = wi + 3
            buf = self.buf
            buf[wi] = (v >> 16) + 0xc0
            buf[wi + 1] = (v >> 8) & 0xff
            buf[wi + 2] = v & 0xff
        elif v < 0x1000_0000:  # 1110 xxxx +3B
            self.ensure_write(4)
            wi = self.wi
            self.wi = wi + 4
            struct.pack_into(">I", self.buf, wi, v + 0xe000_0000)
        else:  # 1111 0000 +4B
            self.ensure_write(5)
            wi = self.wi
            self.wi = wi + 5
            buf = self.buf
            buf[wi] = 0xf0
            struct.pack_into(">I", self.buf, wi + 1, v)

    @staticmethod
    def truncate_int(x):
        x &= 0xffff_ffff
        return x if x <= 0x7fff_ffff else x - 0x1_0000_0000

    @staticmethod
    def truncate_long(x):
        x &= 0xffff_ffff_ffff_ffff
        return x if x <= 0x7fff_ffff_ffff_ffff else x - 0x1_0000_0000_0000_0000

    def read_uint(self):
        return ByteBuffer.truncate_int(self.read_ulong())

    def skip_uint(self):
        self.ensure_read(1)
        ri = self.ri
        v = self.buf[ri]
        if v < 0x80:
            self.ri = ri + 1
        elif v < 0xc0:
            self.ensure_read(2)
            self.ri = ri + 2
        elif v < 0xe0:
            self.ensure_read(3)
            self.ri = ri + 3
        elif v < 0xf0:
            self.ensure_read(4)
            self.ri = ri + 4
        else:
            self.ensure_read(5)
            self.ri = ri + 5

    @staticmethod
    def write_long_size(v):
        # @formatter:off
        if v >= 0:
            if v <                0x40: return 1
            if v <              0x2000: return 2
            if v <           0x10_0000: return 3
            if v <          0x800_0000: return 4
            if v <       0x4_0000_0000: return 5
            if v <     0x200_0000_0000: return 6
            if v <  0x1_0000_0000_0000: return 7
            if v < 0x80_0000_0000_0000: return 8
        else:
            if v >= -               0x40: return 1
            if v >= -             0x2000: return 2
            if v >= -          0x10_0000: return 3
            if v >= -         0x800_0000: return 4
            if v >= -      0x4_0000_0000: return 5
            if v >= -    0x200_0000_0000: return 6
            if v >= - 0x1_0000_0000_0000: return 7
            if v >= -0x80_0000_0000_0000: return 8
        return 9
        # @formatter:on

    def write_long(self, v):
        if v >= 0:
            if v < 0x40:  # 00xx xxxx
                self.ensure_write(1)
                wi = self.wi
                self.wi = wi + 1
                self.buf[wi] = v
            elif v < 0x2000:  # 010x xxxx +1B
                self.ensure_write(2)
                wi = self.wi
                self.wi = wi + 2
                buf = self.buf
                buf[wi] = (v >> 8) + 0x40
                buf[wi + 1] = v & 0xff
            elif v < 0x10_0000:  # 0110 xxxx +2B
                self.ensure_write(3)
                wi = self.wi
                self.wi = wi + 3
                buf = self.buf
                buf[wi] = (v >> 16) + 0x60
                buf[wi + 1] = (v >> 8) & 0xff
                buf[wi + 2] = v & 0xff
            elif v < 0x800_0000:  # 0111 0xxx +3B
                self.ensure_write(4)
                wi = self.wi
                self.wi = wi + 4
                struct.pack_into(">i", self.buf, wi, v + 0x7000_0000)
            elif v < 0x4_0000_0000:  # 0111 10xx +4B
                self.ensure_write(5)
                wi = self.wi
                self.wi = wi + 5
                buf = self.buf
                buf[wi] = (v >> 32) + 0x78
                struct.pack_into(">I", buf, wi + 1, v & 0xffff_ffff)
            elif v < 0x200_0000_0000:  # 0111 110x +5B
                self.ensure_write(6)
                wi = self.wi
                self.wi = wi + 6
                buf = self.buf
                buf[wi] = (v >> 40) + 0x7c
                buf[wi + 1] = (v >> 32) & 0xff
                struct.pack_into(">I", buf, wi + 2, v & 0xffff_ffff)
            elif v < 0x1_0000_0000_0000:  # 0111 1110 +6B
                self.ensure_write(7)
                wi = self.wi
                self.wi = wi + 7
                buf = self.buf
                buf[wi] = 0x7e
                buf[wi + 1] = v >> 40
                buf[wi + 2] = (v >> 32) & 0xff
                struct.pack_into(">I", buf, wi + 3, v & 0xffff_ffff)
            elif v < 0x80_0000_0000_0000:  # 0111 1111 0 +7B
                self.ensure_write(8)
                wi = self.wi
                self.wi = wi + 8
                struct.pack_into(">Q", self.buf, wi, v + 0x7f00_0000_0000_0000)
            else:  # 0111 1111 1 +8B
                self.ensure_write(9)
                wi = self.wi
                self.wi = wi + 9
                buf = self.buf
                buf[wi] = 0x7f
                struct.pack_into(">Q", buf, wi + 1, v + 0x8000_0000_0000_0000)
        else:
            if v >= -0x40:  # 11xx xxxx
                self.ensure_write(1)
                wi = self.wi
                self.wi = wi + 1
                self.buf[wi] = v & 0xff
            elif v >= -0x2000:  # 101x xxxx +1B
                self.ensure_write(2)
                wi = self.wi
                self.wi = wi + 2
                buf = self.buf
                buf[wi] = ((v >> 8) - 0x40) & 0xff
                buf[wi + 1] = v & 0xff
            elif v >= -0x10_0000:  # 1001 xxxx +2B
                self.ensure_write(3)
                wi = self.wi
                self.wi = wi + 3
                buf = self.buf
                buf[wi] = ((v >> 16) - 0x60) & 0xff
                buf[wi + 1] = (v >> 8) & 0xff
                buf[wi + 2] = v & 0xff
            elif v >= -0x800_0000:  # 1000 1xxx +3B
                self.ensure_write(4)
                wi = self.wi
                self.wi = wi + 4
                struct.pack_into(">i", self.buf, wi, v - 0x7000_0000)
            elif v >= -0x4_0000_0000:  # 1000 01xx +4B
                self.ensure_write(5)
                wi = self.wi
                self.wi = wi + 5
                buf = self.buf
                buf[wi] = ((v >> 32) - 0x78) & 0xff
                struct.pack_into(">I", buf, wi + 1, v & 0xffff_ffff)
            elif v >= -0x200_0000_0000:  # 1000 001x +5B
                self.ensure_write(6)
                wi = self.wi
                self.wi = wi + 6
                buf = self.buf
                buf[wi] = ((v >> 40) - 0x7c) & 0xff
                buf[wi + 1] = (v >> 32) & 0xff
                struct.pack_into(">I", buf, wi + 2, v & 0xffff_ffff)
            elif v >= -0x1_0000_0000_0000:  # 1000 0001 +6B
                self.ensure_write(7)
                wi = self.wi
                self.wi = wi + 7
                buf = self.buf
                buf[wi] = 0x81
                buf[wi + 1] = (v >> 40) & 0xff
                buf[wi + 2] = (v >> 32) & 0xff
                struct.pack_into(">I", buf, wi + 3, v & 0xffff_ffff)
            elif v >= -0x80_0000_0000_0000:  # 1000 0000 1 +7B
                self.ensure_write(8)
                wi = self.wi
                self.wi = wi + 8
                struct.pack_into(">q", self.buf, wi, v - 0x7f00_0000_0000_0000)
            else:  # 1000 0000 0 +8B
                self.ensure_write(9)
                wi = self.wi
                self.wi = wi + 9
                buf = self.buf
                buf[wi] = 0x80
                struct.pack_into(">q", self.buf, wi + 1, v + 0x8000_0000_0000_0000)

    @staticmethod
    def write_ulong_size(v):
        # @formatter:off
        if v <                 0x80: return 1 if v >= 0 else 9
        if v <               0x4000: return 2
        if v <            0x20_0000: return 3
        if v <          0x1000_0000: return 4
        if v <        0x8_0000_0000: return 5
        if v <      0x400_0000_0000: return 6
        if v <   0x2_0000_0000_0000: return 7
        if v < 0x100_0000_0000_0000: return 8
        return 9
        # @formatter:on

    def write_ulong(self, v):
        if v < 0x80:
            if v >= 0:  # 0xxx xxxx
                self.ensure_write(1)
                wi = self.wi
                self.wi = wi + 1
                self.buf[wi] = v
                return
        else:
            if v < 0x4000:  # 10xx xxxx +1B
                self.ensure_write(2)
                wi = self.wi
                self.wi = wi + 2
                buf = self.buf
                buf[wi] = ((v >> 8) + 0x80)
                buf[wi + 1] = v & 0xff
                return
            if v < 0x20_0000:  # 110x xxxx +2B
                self.ensure_write(3)
                wi = self.wi
                self.wi = wi + 3
                buf = self.buf
                buf[wi] = (v >> 16) + 0xc0
                buf[wi + 1] = (v >> 8) & 0xff
                buf[wi + 2] = v & 0xff
                return
            if v < 0x1000_0000:  # 1110 xxxx +3B
                self.ensure_write(4)
                wi = self.wi
                self.wi = wi + 4
                struct.pack_into(">I", self.buf, wi, v + 0xe000_0000)
                return
            if v < 0x8_0000_0000:  # 1111 0xxx +4B
                self.ensure_write(5)
                wi = self.wi
                self.wi = wi + 5
                buf = self.buf
                buf[wi] = (v >> 32) + 0xf0
                struct.pack_into(">I", buf, wi + 1, v & 0xffff_ffff)
                return
            if v < 0x400_0000_0000:  # 1111 10xx +5B
                self.ensure_write(6)
                wi = self.wi
                self.wi = wi + 6
                buf = self.buf
                buf[wi] = (v >> 40) + 0xf8
                buf[wi + 1] = (v >> 32) & 0xff
                struct.pack_into(">I", buf, wi + 2, v & 0xffff_ffff)
                return
            if v < 0x2_0000_0000_0000:  # 1111 110x +6B
                self.ensure_write(7)
                wi = self.wi
                self.wi = wi + 7
                buf = self.buf
                buf[wi] = (v >> 48) + 0xfc
                buf[wi + 1] = (v >> 40) & 0xff
                buf[wi + 2] = (v >> 32) & 0xff
                struct.pack_into(">I", buf, wi + 3, v & 0xffff_ffff)
                return
            if v < 0x100_0000_0000_0000:  # 1111 1110 +7B
                self.ensure_write(8)
                wi = self.wi
                self.wi = wi + 8
                struct.pack_into(">Q", self.buf, wi, v + 0xfe00_0000_0000_0000)
                return
        self.ensure_write(9)  # 1111 1111 +8B
        wi = self.wi
        self.wi = wi + 9
        buf = self.buf
        buf[wi] = 0xff
        struct.pack_into(">Q", self.buf, wi + 1, v & 0xffff_ffff_ffff_ffff)

    def read_ulong(self):
        b = self.read_byte()
        h = b >> 4
        # @formatter:off
        if h < 8:  return b
        if h < 12: return ((b & 0x3f) <<  8) + self.read_long1()
        if h < 14: return ((b & 0x1f) << 16) + self.read_long2_be()
        if h < 15: return ((b & 0x0f) << 24) + self.read_long3_be()
        h = b & 0xf
        if h < 8:  return ((b & 7) << 32) + self.read_long4_be()
        if h < 12: return ((b & 3) << 40) + self.read_long5_be()
        if h < 14: return ((b & 1) << 48) + self.read_long6_be()
        if h < 15: return self.read_long7_be()
        # @formatter:on
        return self.read_long8_be()

    def skip_ulong(self):
        b = self.read_byte()
        h = b >> 4
        # @formatter:off
        if h < 8:  return
        if h < 12: self.skip(1); return
        if h < 14: self.skip(2); return
        if h < 15: self.skip(3); return
        h = b & 0xf
        if h < 8:  self.skip(4); return
        if h < 12: self.skip(5); return
        if h < 14: self.skip(6); return
        if h < 15: self.skip(7); return
        # @formatter:on
        self.skip(8)

    def write_vector2(self, v):
        self.ensure_write(8)
        wi = self.wi
        self.wi = wi + 8
        struct.pack_into("<ff", self.buf, wi, v.x, v.y)

    def write_vector3(self, v):
        self.ensure_write(12)
        wi = self.wi
        self.wi = wi + 12
        struct.pack_into("<fff", self.buf, wi, v.x, v.y, v.z)

    def write_vector4(self, v):
        self.ensure_write(16)
        wi = self.wi
        self.wi = wi + 16
        struct.pack_into("<ffff", self.buf, wi, v.x, v.y, v.z, v.w)

    def write_quaternion(self, v):
        self.write_vector4(v)

    def write_vector2int(self, v):
        self.write_int(v.x)
        self.write_int(v.y)

    def write_vector3int(self, v):
        self.write_int(v.x)
        self.write_int(v.y)
        self.write_int(v.z)

    def read_long1(self):
        self.ensure_read(1)
        ri = self.ri
        self.ri = ri + 1
        return self.buf[ri]

    def read_long2_be(self):
        self.ensure_read(2)
        buf = self.buf
        ri = self.ri
        self.ri = ri + 2
        return ((buf[ri] << 8) +
                buf[ri + 1])

    def read_long3_be(self):
        self.ensure_read(3)
        buf = self.buf
        ri = self.ri
        self.ri = ri + 3
        return ((buf[ri] << 16) +
                (buf[ri + 1] << 8) +
                buf[ri + 2])

    def read_long4_be(self) -> int:
        self.ensure_read(4)
        ri = self.ri
        self.ri = ri + 4
        # noinspection PyTypeChecker
        return struct.unpack_from(">I", self.buf, ri)[0]

    def read_long5_be(self):
        self.ensure_read(5)
        buf = self.buf
        ri = self.ri
        self.ri = ri + 5
        # noinspection PyTypeChecker
        return ((buf[ri] << 32) +
                struct.unpack_from(">I", buf, ri + 1)[0])

    def read_long6_be(self):
        self.ensure_read(6)
        buf = self.buf
        ri = self.ri
        self.ri = ri + 6
        # noinspection PyTypeChecker
        return ((buf[ri] << 40) +
                (buf[ri + 1] << 32) +
                struct.unpack_from(">I", buf, ri + 2)[0])

    def read_long7_be(self):
        self.ensure_read(7)
        buf = self.buf
        ri = self.ri
        self.ri = ri + 7
        # noinspection PyTypeChecker
        return ((buf[ri] << 48) +
                (buf[ri + 1] << 40) +
                (buf[ri + 2] << 32) +
                struct.unpack_from(">I", buf, ri + 3)[0])

    def read_long8_be(self) -> int:
        self.ensure_read(8)
        buf = self.buf
        ri = self.ri
        self.ri = ri + 8
        # noinspection PyTypeChecker
        return struct.unpack_from(">q", buf, ri)[0]

    def read_long(self):
        self.ensure_read(1)
        ri = self.ri
        self.ri = ri + 1
        b = self.buf[ri]
        h = b >> 3
        # @formatter:off
        if h < 0x10:
            if h < 0x08:  return b
            if h < 0x0c:  return ((b - 0x40) << 8) + self.read_long1()
            if h < 0x0e:  return ((b - 0x60) << 16) + self.read_long2_be()
            if h == 0x0e: return ((b - 0x70) << 24) + self.read_long3_be()
            h = b & 7
            if h < 4:  return ((b - 0x78) << 32) + self.read_long4_be()
            if h < 6:  return ((b - 0x7c) << 40) + self.read_long5_be()
            if h == 6: return self.read_long6_be()
            r = self.read_long7_be()
            return r if r < 0x80_0000_0000_0000 else ((r - 0x80_0000_0000_0000) << 8) + self.read_long1()
        else:
            b -= 0x100
            if h >= 0x18: return b
            if h >= 0x14: return ((b + 0x40) << 8) + self.read_long1()
            if h >= 0x12: return ((b + 0x60) << 16) + self.read_long2_be()
            if h == 0x11: return ((b + 0x70) << 24) + self.read_long3_be()
            h = b & 7
            if h >= 4: return ((b + 0x78) << 32) + self.read_long4_be()
            if h >= 2: return ((b + 0x7c) << 40) + self.read_long5_be()
            if h == 1: return -0x1_0000_0000_0000 + self.read_long6_be()
            r = self.read_long7_be()
            return -0x100_0000_0000_0000 + r if r >= 0x80_0000_0000_0000 else ((r - 0x80_0000_0000_0000) << 8) + self.read_long1()
        # @formatter:on

    def skip_long(self):
        self.ensure_read(1)
        ri = self.ri
        self.ri = ri + 1
        b = self.buf[ri]
        h = b >> 3
        # @formatter:off
        if h < 0x10:
            if h < 0x08:  return
            if h < 0x0c:  self.skip(1); return
            if h < 0x0e:  self.skip(2); return
            if h == 0x0e: self.skip(3); return
            h = b & 7
            if h < 4:  self.skip(4); return
            if h < 6:  self.skip(5); return
            if h == 6: self.skip(6); return
            self.ensure_read(1)
            self.ri = ri + 2
            self.skip(6 + (self.buf[ri + 1] > 0x7f))
            return
        else:
            b -= 0x100
            if h >= 0x18: return
            if h >= 0x14: self.skip(1); return
            if h >= 0x12: self.skip(2); return
            if h == 0x11: self.skip(3); return
            h = b & 7
            if h >= 4: self.skip(4); return
            if h >= 2: self.skip(5); return
            if h == 1: self.skip(6); return
            self.ensure_read(1)
            self.ri = ri + 2
            self.skip(7 - (self.buf[ri + 1] > 0x7f))
        # @formatter:on

    def write_int(self, v):
        self.write_long(v)

    def read_int(self):
        return ByteBuffer.truncate_int(self.read_long())

    def write_float(self, v):
        self.ensure_write(4)
        wi = self.wi
        self.wi = wi + 4
        struct.pack_into("<f", self.buf, wi, v)

    def read_float(self):
        self.ensure_read(4)
        ri = self.ri
        self.ri = ri + 4
        return struct.unpack_from("<f", self.buf, ri)[0]

    def write_double(self, v):
        self.ensure_write(8)
        wi = self.wi
        self.wi = wi + 8
        struct.pack_into("<d", self.buf, wi, v)

    def read_double(self):
        self.ensure_read(8)
        ri = self.ri
        self.ri = ri + 8
        return struct.unpack_from("<d", self.buf, ri)[0]

    @staticmethod
    def utf8_size(s):
        if type(s) != str:
            return 0
        bn = 0
        for c in s:
            v = ord(c)
            if v < 0x80:
                bn += 1
            elif v < 0x800:
                bn += 2
            elif v < 0x10000:
                bn += 3
            else:
                bn += 4
        return bn

    def write_string(self, s):
        bn = ByteBuffer.utf8_size(s)
        if bn <= 0:
            self.write_byte(0)
            return
        self.write_uint(bn)
        self.ensure_write(bn)
        buf = self.buf
        wi = self.wi
        if bn == len(s):
            for c in s:
                buf[wi] = ord(c)
                wi += 1
        else:
            for c in s:
                v = ord(c)
                if v < 0x80:
                    buf[wi] = v
                    wi += 1
                elif v < 0x800:
                    buf[wi] = 0xc0 + (v >> 6)
                    buf[wi + 1] = 0x80 + (v & 0x3f)
                    wi += 2
                elif v < 0x10000:
                    buf[wi] = 0xe0 + (v >> 12)
                    buf[wi + 1] = 0x80 + ((v >> 6) & 0x3f)
                    buf[wi + 2] = 0x80 + (v & 0x3f)
                    wi += 3
                else:
                    buf[wi] = 0xf0 + (v >> 18)
                    buf[wi + 1] = 0x80 + ((v >> 12) & 0x3f)
                    buf[wi + 2] = 0x80 + ((v >> 6) & 0x3f)
                    buf[wi + 3] = 0x80 + (v & 0x3f)
                    wi += 4
        self.wi = wi

    def read_string(self):
        n = self.read_uint()
        if n == 0:
            return ""
        if n < 0:
            raise Exception(f"invalid length for read_string: {n} at {self.ri}/{self.wi}")
        self.ensure_read(n)
        ri = self.ri
        self.ri = ri + n
        return self.buf[ri:ri + n].decode('utf-8')

    def write_bytes(self, b, offset=0, length=-1):
        if length < 0:
            length = len(b)
        self.write_uint(length)
        self.ensure_write(length)
        wi = self.wi
        self.wi = wi + length
        self.buf[wi:wi + length] = b[offset:offset + length]

    def read_bytes(self):
        n = self.read_uint()
        if n == 0:
            return ByteBuffer.empty
        if n < 0:
            raise Exception(f"invalid length for read_bytes: {n} at {self.ri}/{self.wi}")
        self.ensure_read(n)
        b = bytearray(n)
        ri = self.ri
        self.ri = ri + n
        b[0:n] = self.buf[ri:ri + n]
        return b

    def skip(self, n):
        self.ensure_read(n)
        self.ri += n

    def skip_bytes(self):
        n = self.read_uint()
        if n < 0:
            raise Exception(f"invalid length for skip_bytes: {n} at {self.ri}/{self.wi}")
        self.skip(n)

    def skip_bytes4(self):
        n = self.read_int4()
        if n < 0:
            raise Exception(f"invalid length for skip_bytes4: {n} at {self.ri}/{self.wi}")
        self.skip(n)

    def read_bytebuffer(self):
        # 会推进ri, 但是返回的ByteBuffer和原来的共享内存
        n = self.read_uint()
        if n < 0:
            raise Exception(f"invalid length for read_bytebuffer: {n} at {self.ri}/{self.wi}")
        self.ensure_read(n)
        ri = self.ri
        self.ri = ri + n
        return ByteBuffer.wrap(self.buf, ri, n)

    def write_bytebuffer(self, bb):
        self.write_bytes(bb.buf, bb.ri, bb.size())

    def __str__(self, *args, **kwargs):
        return to_string_with_limit2(16, 4, self.buf, self.ri, self.size())

    def __eq__(self, other):
        if isinstance(other, ByteBuffer):
            return self is other or self.buf[self.ri: self.wi] == other.buf[other.ri: other.wi]
        if isinstance(other, bytearray) or isinstance(other, bytes):
            return self.buf[self.ri: self.wi] == other
        return False

    def __hash__(self):
        return ByteBuffer.calc_hashnr(self.buf, self.ri, self.size())

    @staticmethod
    def calc_hashnr(obj, offset=0, length=-1):
        if isinstance(obj, int):
            return ((obj * 0x9E3779B97F4A7C15) >> 32) & 0xffff_ffff
        if length < 0:
            length = len(obj)
        h = 0
        if isinstance(obj, str):
            for i in range(offset, offset + length):
                h = ((h * 16777619) & 0xffff_ffff) ^ ord(obj[i])
        else:
            for i in range(offset, offset + length):
                h = ((h * 16777619) & 0xffff_ffff) ^ obj[i]
        return h

    # 只能增加新的类型定义, 增加时记得同步skip_unknown_field
    INTEGER = 0  # byte,short,int,long,bool
    FLOAT = 1  # float
    DOUBLE = 2  # double
    BYTES = 3  # binary,string
    LIST = 4  # list,set
    MAP = 5  # map
    BEAN = 6  # bean
    DYNAMIC = 7  # dynamic
    VECTOR2 = 8  # float{x,y}
    VECTOR2INT = 9  # int{x,y}
    VECTOR3 = 10  # float{x,y,z}
    VECTOR3INT = 11  # int{x,y,z}
    VECTOR4 = 12  # float{x,y,z,w} Quaternion

    TAG_SHIFT = 4
    TAG_MASK = (1 << TAG_SHIFT) - 1
    ID_MASK = 0xff - TAG_MASK
    IGNORE_INCOMPATIBLE_FIELD = False  # 不忽略兼容字段则会抛异常

    @staticmethod
    def encode(ser):
        pre_alloc_size = ser.get_pre_alloc_size()
        bb = ByteBuffer(min(pre_alloc_size, 0x10000))
        ser.encode(bb)
        if pre_alloc_size < bb.wi:
            ser.set_pre_alloc_size(bb.wi)
        return bb

    def encode_collection(self, c):
        self.write_uint(len(c))
        for s in c:
            s.encode(self)

    def decode_collection(self, c, factory):
        for i in range(0, self.read_uint()):
            v = factory.get()
            v.decode(self)
            c.append(v)

    def write_tag(self, last_var_id, var_id, var_type):
        delta_id = var_id - last_var_id
        if delta_id < 0xf:
            self.write_byte((delta_id << ByteBuffer.TAG_SHIFT) + var_type)
        else:
            self.write_byte(0xf0 + var_type)
            self.write_uint(delta_id - 0xf)
        return var_id

    def write_list_type(self, list_size, elem_type):
        if list_size < 0xf:
            self.write_byte((list_size << ByteBuffer.TAG_SHIFT) + elem_type)
        else:
            self.write_byte(0xf0 + elem_type)
            self.write_uint(list_size - 0xf)

    def write_map_type(self, map_size, key_type, value_type):
        self.write_byte((key_type << ByteBuffer.TAG_SHIFT) + value_type)
        self.write_uint(map_size)

    def read_tag_size(self, tag_byte):
        delta_id = (tag_byte & ByteBuffer.ID_MASK) >> ByteBuffer.TAG_SHIFT
        return delta_id if delta_id < 0xf else 0xf + self.read_uint()

    def read_bool_tag(self, tag):
        t = tag & ByteBuffer.TAG_MASK
        if t == ByteBuffer.INTEGER:
            return self.read_long() != 0
        if t == ByteBuffer.FLOAT:
            return self.read_float() != 0
        if t == ByteBuffer.DOUBLE:
            return self.read_double() != 0
        if ByteBuffer.IGNORE_INCOMPATIBLE_FIELD:
            self.skip_unknown_field(tag)
            return False
        raise Exception(f"can not read_bool_tag for type={t} at {self.ri}/{self.wi}")

    def read_long_tag(self, tag):
        t = tag & ByteBuffer.TAG_MASK
        if t == ByteBuffer.INTEGER:
            return self.read_long()
        if t == ByteBuffer.FLOAT:
            return int(self.read_float())
        if t == ByteBuffer.DOUBLE:
            return int(self.read_double())
        if ByteBuffer.IGNORE_INCOMPATIBLE_FIELD:
            self.skip_unknown_field(tag)
            return 0
        raise Exception(f"can not read_long_tag for type={t} at {self.ri}/{self.wi}")

    def read_float_tag(self, tag):
        t = tag & ByteBuffer.TAG_MASK
        if t == ByteBuffer.FLOAT:
            return self.read_float()
        if t == ByteBuffer.DOUBLE:
            return self.read_double()
        if t == ByteBuffer.INTEGER:
            return float(self.read_long())
        if ByteBuffer.IGNORE_INCOMPATIBLE_FIELD:
            self.skip_unknown_field(tag)
            return 0.0
        raise Exception(f"can not read_float_tag for type={t} at {self.ri}/{self.wi}")

    def read_double_tag(self, tag):
        t = tag & ByteBuffer.TAG_MASK
        if t == ByteBuffer.DOUBLE:
            return self.read_double()
        if t == ByteBuffer.FLOAT:
            return self.read_float()
        if t == ByteBuffer.INTEGER:
            return float(self.read_long())
        if ByteBuffer.IGNORE_INCOMPATIBLE_FIELD:
            self.skip_unknown_field(tag)
            return 0.0
        raise Exception(f"can not read_double_tag for type={t} at {self.ri}/{self.wi}")

    def read_binary_tag(self, tag):
        t = tag & ByteBuffer.TAG_MASK
        if t == ByteBuffer.BYTES:
            return self.read_bytes()
        if ByteBuffer.IGNORE_INCOMPATIBLE_FIELD:
            self.skip_unknown_field(tag)
            return ByteBuffer.empty
        raise Exception(f"can not read_binary_tag for type={t} at {self.ri}/{self.wi}")

    def read_string_tag(self, tag):
        t = tag & ByteBuffer.TAG_MASK
        if t == ByteBuffer.BYTES:
            return self.read_string()
        if ByteBuffer.IGNORE_INCOMPATIBLE_FIELD:
            self.skip_unknown_field(tag)
            return ""
        raise Exception(f"can not read_string_tag for type={t} at {self.ri}/{self.wi}")

    def read_vector2(self):
        self.ensure_read(8)
        ri = self.ri
        self.ri = ri + 8
        x, y = struct.unpack_from("<ff", self.buf, ri)
        return Vector2(x, y)

    def read_vector3(self):
        self.ensure_read(12)
        ri = self.ri
        self.ri = ri + 12
        x, y, z = struct.unpack_from("<fff", self.buf, ri)
        return Vector3(x, y, z)

    def read_vector4(self):
        self.ensure_read(16)
        ri = self.ri
        self.ri = ri + 16
        x, y, z, w = struct.unpack_from("<ffff", self.buf, ri)
        return Vector4(x, y, z, w)

    def read_quaternion(self):
        self.ensure_read(16)
        ri = self.ri
        self.ri = ri + 16
        x, y, z, w = struct.unpack_from("<ffff", self.buf, ri)
        return Quaternion(x, y, z, w)

    def read_vector2int(self):
        x = self.read_int()
        y = self.read_int()
        return Vector2Int(x, y)

    def read_vector3int(self):
        x = self.read_int()
        y = self.read_int()
        z = self.read_int()
        return Vector3Int(x, y, z)

    def read_vector2_tag(self, tag):
        t = tag & ByteBuffer.TAG_MASK
        if t == ByteBuffer.VECTOR2:
            return self.read_vector2()
        if t == ByteBuffer.VECTOR3:
            return self.read_vector3()
        if t == ByteBuffer.VECTOR4:
            return self.read_vector4()
        if t == ByteBuffer.VECTOR2INT:
            return Vector2(self.read_vector2int())
        if t == ByteBuffer.VECTOR3INT:
            return Vector3(self.read_vector3int())
        if t == ByteBuffer.FLOAT:
            return Vector2(self.read_float())
        if t == ByteBuffer.DOUBLE:
            return Vector2(self.read_double())
        if t == ByteBuffer.INTEGER:
            return Vector2(self.read_long())
        if ByteBuffer.IGNORE_INCOMPATIBLE_FIELD:
            self.skip_unknown_field(tag)
            return Vector2(0, 0)
        raise Exception(f"can not read_vector2_tag for type={t} at {self.ri}/{self.wi}")

    def read_vector3_tag(self, tag):
        t = tag & ByteBuffer.TAG_MASK
        if t == ByteBuffer.VECTOR3:
            return self.read_vector3()
        if t == ByteBuffer.VECTOR2:
            return Vector3(self.read_vector2())
        if t == ByteBuffer.VECTOR4:
            return self.read_vector4()
        if t == ByteBuffer.VECTOR3INT:
            return Vector3(self.read_vector3int())
        if t == ByteBuffer.VECTOR2INT:
            return Vector3(self.read_vector2int())
        if t == ByteBuffer.FLOAT:
            return Vector3(self.read_float())
        if t == ByteBuffer.DOUBLE:
            return Vector3(self.read_double())
        if t == ByteBuffer.INTEGER:
            return Vector3(self.read_long())
        if ByteBuffer.IGNORE_INCOMPATIBLE_FIELD:
            self.skip_unknown_field(tag)
            return Vector3(0, 0, 0)
        raise Exception(f"can not read_vector3_tag for type={t} at {self.ri}/{self.wi}")

    def read_vector4_tag(self, tag):
        t = tag & ByteBuffer.TAG_MASK
        if t == ByteBuffer.VECTOR4:
            return self.read_vector4()
        if t == ByteBuffer.VECTOR3:
            return Vector4(self.read_vector3())
        if t == ByteBuffer.VECTOR2:
            return Vector4(self.read_vector2())
        if t == ByteBuffer.VECTOR3INT:
            return Vector4(self.read_vector3int())
        if t == ByteBuffer.VECTOR2INT:
            return Vector4(self.read_vector2int())
        if t == ByteBuffer.FLOAT:
            return Vector4(self.read_float())
        if t == ByteBuffer.DOUBLE:
            return Vector4(self.read_double())
        if t == ByteBuffer.INTEGER:
            return Vector4(self.read_long())
        if ByteBuffer.IGNORE_INCOMPATIBLE_FIELD:
            self.skip_unknown_field(tag)
            return Vector4(0, 0, 0, 0)
        raise Exception(f"can not read_vector4_tag for type={t} at {self.ri}/{self.wi}")

    def read_quaternion_tag(self, tag):
        t = tag & ByteBuffer.TAG_MASK
        if t == ByteBuffer.VECTOR4:
            return self.read_quaternion()
        if t == ByteBuffer.VECTOR3:
            return Quaternion(self.read_vector3())
        if t == ByteBuffer.VECTOR2:
            return Quaternion(self.read_vector2())
        if t == ByteBuffer.VECTOR3INT:
            return Quaternion(self.read_vector3int())
        if t == ByteBuffer.VECTOR2INT:
            return Quaternion(self.read_vector2int())
        if t == ByteBuffer.FLOAT:
            return Quaternion(self.read_float())
        if t == ByteBuffer.DOUBLE:
            return Quaternion(self.read_double())
        if t == ByteBuffer.INTEGER:
            return Quaternion(self.read_long())
        if ByteBuffer.IGNORE_INCOMPATIBLE_FIELD:
            self.skip_unknown_field(tag)
            return Quaternion(0, 0, 0, 0)
        raise Exception(f"can not read_quaternion_tag for type={t} at {self.ri}/{self.wi}")

    def read_vector2int_tag(self, tag):
        t = tag & ByteBuffer.TAG_MASK
        if t == ByteBuffer.VECTOR2INT:
            return self.read_vector2int()
        if t == ByteBuffer.VECTOR3INT:
            return self.read_vector3int()
        if t == ByteBuffer.VECTOR2:
            return Vector2Int(self.read_vector2())
        if t == ByteBuffer.VECTOR3:
            return Vector3Int(self.read_vector3())
        if t == ByteBuffer.VECTOR4:
            return Vector3Int(self.read_vector4())
        if t == ByteBuffer.INTEGER:
            return Vector2Int(self.read_long())
        if t == ByteBuffer.FLOAT:
            return Vector2Int(self.read_float())
        if t == ByteBuffer.DOUBLE:
            return Vector2Int(self.read_double())
        if ByteBuffer.IGNORE_INCOMPATIBLE_FIELD:
            self.skip_unknown_field(tag)
            return Vector2Int(0, 0)
        raise Exception(f"can not read_vector2int_tag for type={t} at {self.ri}/{self.wi}")

    def read_vector3int_tag(self, tag):
        t = tag & ByteBuffer.TAG_MASK
        if t == ByteBuffer.VECTOR3INT:
            return self.read_vector3int()
        if t == ByteBuffer.VECTOR2INT:
            return Vector3Int(self.read_vector2int())
        if t == ByteBuffer.VECTOR3:
            return Vector3Int(self.read_vector3())
        if t == ByteBuffer.VECTOR2:
            return Vector3Int(self.read_vector2())
        if t == ByteBuffer.VECTOR4:
            return Vector3Int(self.read_vector4())
        if t == ByteBuffer.INTEGER:
            return Vector3Int(self.read_long())
        if t == ByteBuffer.FLOAT:
            return Vector3Int(self.read_float())
        if t == ByteBuffer.DOUBLE:
            return Vector3Int(self.read_double())
        if ByteBuffer.IGNORE_INCOMPATIBLE_FIELD:
            self.skip_unknown_field(tag)
            return Vector3Int(0, 0)
        raise Exception(f"can not read_vector3int_tag for type={t} at {self.ri}/{self.wi}")

    def read_bean_tag(self, bean, tag):
        t = tag & ByteBuffer.TAG_MASK
        if t == ByteBuffer.BEAN:
            bean.decode(self)
        elif t == ByteBuffer.DYNAMIC:
            self.skip_long()
            bean.decode(self)
        elif ByteBuffer.IGNORE_INCOMPATIBLE_FIELD:
            self.skip_unknown_field(tag)
        else:
            raise Exception(f"can not read_bean_tag({type(bean)}) for type={t} at {self.ri}/{self.wi}")
        return bean

    def read_dynamic_tag(self, id2bean, tag):
        t = tag & ByteBuffer.TAG_MASK
        if t == ByteBuffer.DYNAMIC:
            bean = id2bean(self.read_long())
            bean.decode(self)
        elif t == ByteBuffer.BEAN:
            bean = id2bean(0)
            bean.decode(self)
        elif ByteBuffer.IGNORE_INCOMPATIBLE_FIELD:
            self.skip_unknown_field(tag)
            from zeze.bean import EmptyBean
            bean = EmptyBean()
        else:
            raise Exception(f"can not read_dynamic_tag for type={t} at {self.ri}/{self.wi}")
        return bean

    def skip_unknown_field_or_raise(self, tag, cur_type):
        if ByteBuffer.IGNORE_INCOMPATIBLE_FIELD:
            self.skip_unknown_field(tag)
        else:
            raise Exception(f"can not read {cur_type} for type={tag & ByteBuffer.TAG_MASK} at {self.ri}/{self.wi}")

    def skip_unknown_fields(self, tag, count):
        while count > 0:
            count -= 1
            self.skip_unknown_field(tag)

    def skip_unknown_field_kvs(self, type1, type2, count):
        type1 |= 0x10  # ensure high bits not zero
        type2 |= 0x10  # ensure high bits not zero
        while count > 0:
            self.skip_unknown_field(type1)
            self.skip_unknown_field(type2)

    def skip_unknown_field(self, tag):
        t = tag & ByteBuffer.TAG_MASK
        if t == ByteBuffer.INTEGER:
            self.skip_long()
            return
        if t == ByteBuffer.FLOAT:
            if tag == ByteBuffer.FLOAT:  # high bits == 0
                return
            self.skip(4)
            return
        if t == ByteBuffer.DOUBLE or t == ByteBuffer.VECTOR2:
            self.skip(8)
            return
        if t == ByteBuffer.VECTOR2INT:
            self.skip_long()
            self.skip_long()
            return
        if t == ByteBuffer.VECTOR3:
            self.skip(12)
            return
        if t == ByteBuffer.VECTOR3INT:
            self.skip_long()
            self.skip_long()
            self.skip_long()
            return
        if t == ByteBuffer.VECTOR4:
            self.skip(16)
            return
        if t == ByteBuffer.BYTES:
            self.skip_bytes()
            return
        if t == ByteBuffer.LIST:
            tb = self.read_byte()
            self.skip_unknown_fields(tb, self.read_tag_size(tb))
            return
        if t == ByteBuffer.MAP:
            tb = self.read_byte()
            self.skip_unknown_field_kvs(tb >> ByteBuffer.TAG_SHIFT, tb, self.read_uint())
            return
        if t == ByteBuffer.DYNAMIC:
            self.skip_long()
        elif t != ByteBuffer.BEAN:
            raise Exception(f"skip_unknown_field: type={t} at {self.ri}/{self.wi}")
        tb = self.read_byte()
        while tb != 0:
            if (tb & ByteBuffer.ID_MASK) == 0xf0:
                self.skip_uint()
            self.skip_unknown_field(tb)
            tb = self.read_byte()

    def skip_all_unknown_fields(self, tag):
        while tag != 0:
            self.skip_unknown_field(tag)
            tag = self.read_byte()
            self.read_tag_size(tag)

    def read_unknown_field(self, idx, tag, unknown=None):
        ri = self.ri
        self.skip_unknown_field(tag)
        size = self.ri - ri
        if size > 0:
            if unknown is None:
                unknown = ByteBuffer(bytearray(16))
            unknown.write_uint(idx)
            unknown.write_byte(tag & ByteBuffer.TAG_MASK)
            unknown.write_uint(size)
            unknown.append(self.buf, ri, size)
            return unknown
        raise Exception("read_unknown_field: unsupported for derived bean")

    def read_all_unknown_fields(self, idx, tag, unknown=None):
        while tag != 0:
            unknown = self.read_unknown_field(idx, tag, unknown)
            tag = self.read_byte()
            idx += self.read_tag_size(tag)
        return None if unknown is None else unknown.copy_if()

    def read_unknown_index(self):
        return self.read_uint() if self.ri < self.wi else 0x7fff_ffff_ffff_ffff

    def write_unknown_field(self, last_idx, idx, unknown):
        i = idx
        self.write_tag(last_idx, i, unknown.read_byte())
        size = unknown.read_uint()
        ri = unknown.ri
        self.append(unknown.buf, ri, size)
        unknown.ri = ri + size
        return i

    def write_all_unknown_fields(self, last_idx, idx, unknown):
        while idx != 0x7fff_ffff_ffff_ffff:
            last_idx = self.write_unknown_field(last_idx, idx, unknown)
            idx = unknown.read_unknown_index()
