import random
import unittest

from gen.demo import Bean1
from gen.demo.Module1 import BValue, BSimple, Key
from zeze.buffer import ByteBuffer
from zeze.util import *


class TestByteBuffer(unittest.TestCase):
    def test_util(self):
        bs = bytearray(256)
        for i in range(0, len(bs)):
            bs[i] = i
        self.assertEqual("00-01-02-03-04-05-06-07-08-09", to_string_with_limit(10, bs, 0, 10))
        self.assertEqual("00-01-02-03-04-05-06-07-08-09...[+246]", to_string_with_limit(10, bs))
        self.assertEqual("00-01-02-03-04-05...[+246]...FC-FD-FE-FF", to_string_with_limit2(6, 4, bs))
        self.assertEqual("00-01-02-03-04-05-06-07-08-09", to_string_with_limit2(6, 4, bs, 0, 10))
        self.assertEqual("00-01-02-03-04-05...[+1]...07-08-09-0A", to_string_with_limit2(6, 4, bs, 0, 11))
        self.assertEqual("00-01-02-03-04-05...[+4]", to_string_with_limit2(6, 0, bs, 0, 10))
        self.assertEqual("[+6]...06-07-08-09", to_string_with_limit2(0, 4, bs, 0, 10))
        self.assertEqual("[+10]", to_string_with_limit2(0, 0, bs, 0, 10))

        s = "01-23-45-67-89-ab-cd-ef-AB-CD-EF"
        self.assertEqual(s.upper(), to_string(to_bytes(s)))

    def test_bytes(self):
        bb = ByteBuffer()
        v = ByteBuffer.empty
        bb.write_bytes(v)
        self.assertEqual(1, bb.size())
        self.assertEqual("00", bb.__str__())
        self.assertEqual(to_string(v), to_string(bb.read_bytes()))
        self.assertEqual(bb.ri, bb.wi)

        v = b"\x01\x02"
        bb.write_bytes(v)
        self.assertEqual(3, bb.size())
        self.assertEqual("02-01-02", bb.__str__())
        self.assertEqual(to_string(v), to_string(bb.read_bytes()))
        self.assertEqual(bb.ri, bb.wi)

        s = "abc汉字123"
        bb.write_string(s)
        self.assertEqual(13, bb.size())
        self.assertEqual("0C-61-62-63-E6-B1-89-E5-AD-97-31-32-33", bb.__str__())
        self.assertEqual(s, bb.read_string())
        self.assertEqual(bb.ri, bb.wi)

        s = b"\xd9\x55\xde\xaa\xda\xaa\xdd\x55".decode('utf-16-be')  # surrogate chars
        b0 = s.encode('utf-8')
        bb.reset()
        bb.write_string(s)
        self.assertEqual(4 * 2, len(b0))
        self.assertEqual(4 * 2, ByteBuffer.utf8_size(s))
        self.assertEqual(4 * 2, bb.buf[0])
        self.assertEqual(b0, bb.read_bytes())
        bb.ri = 0
        self.assertEqual(s, bb.read_string())

    def test_basic(self):
        bb = ByteBuffer()
        self.assertEqual(bb.ri, bb.wi)

        v = True
        bb.write_bool(v)
        self.assertEqual(1, bb.size())
        self.assertEqual(1, bb.buf[bb.ri])
        self.assertEqual(v, bb.read_bool())
        self.assertEqual(bb.ri, bb.wi)

        v = 1
        bb.write_byte(v)
        self.assertEqual(1, bb.size())
        self.assertEqual(1, bb.buf[bb.ri])
        self.assertEqual(v, bb.read_byte())
        self.assertEqual(bb.ri, bb.wi)

        v = 1.1
        bb.write_double(v)
        self.assertEqual(8, bb.size())
        self.assertEqual("9A-99-99-99-99-99-F1-3F", bb.__str__())
        self.assertEqual(v, bb.read_double())
        self.assertEqual(bb.ri, bb.wi)

        v = 1.1
        bb.write_float(v)
        self.assertEqual(4, bb.size())
        self.assertEqual("CD-CC-8C-3F", bb.__str__())
        self.assertTrue(abs(bb.read_float() - v) < 0.000001)
        self.assertEqual(bb.ri, bb.wi)

        int4 = 0x12345678
        bb.write_int4(int4)
        self.assertEqual(4, bb.size())
        self.assertEqual("78-56-34-12", bb.__str__())
        self.assertEqual(int4, bb.read_int4())
        self.assertEqual(bb.ri, bb.wi)

        long8 = 0x1234567801020304
        bb.write_long8(long8)
        self.assertEqual(8, bb.size())
        self.assertEqual("04-03-02-01-78-56-34-12", bb.__str__())
        self.assertEqual(long8, bb.read_long8())
        self.assertEqual(bb.ri, bb.wi)

        long8 = -12345678
        bb.write_long8(long8)
        self.assertEqual(8, bb.size())
        self.assertEqual("B2-9E-43-FF-FF-FF-FF-FF", bb.__str__())
        self.assertEqual(long8, bb.read_long8())
        self.assertEqual(bb.ri, bb.wi)

        long8 = -1
        bb.write_long8(long8)
        self.assertEqual(8, bb.size())
        self.assertEqual("FF-FF-FF-FF-FF-FF-FF-FF", bb.__str__())
        self.assertEqual(long8, bb.read_long8())
        self.assertEqual(bb.ri, bb.wi)

    def test_uint(self):
        bb = ByteBuffer()
        self.assertEqual(bb.ri, bb.wi)

        v = 1
        bb.write_uint(v)
        self.assertEqual(1, bb.size())
        self.assertEqual("01", bb.__str__())
        self.assertEqual(v, bb.read_uint())
        self.assertEqual(bb.ri, bb.wi)

        v = 0x80
        bb.write_uint(v)
        self.assertEqual(2, bb.size())
        self.assertEqual("80-80", bb.__str__())
        self.assertEqual(v, bb.read_uint())
        self.assertEqual(bb.ri, bb.wi)

        v = 0x4000
        bb.write_uint(v)
        self.assertEqual(3, bb.size())
        self.assertEqual("C0-40-00", bb.__str__())
        self.assertEqual(v, bb.read_uint())
        self.assertEqual(bb.ri, bb.wi)

        v = 0x20_0000
        bb.write_uint(v)
        self.assertEqual(4, bb.size())
        self.assertEqual("E0-20-00-00", bb.__str__())
        self.assertEqual(v, bb.read_uint())
        self.assertEqual(bb.ri, bb.wi)

        v = 0x1000_0000
        bb.write_uint(v)
        self.assertEqual(5, bb.size())
        self.assertEqual("F0-10-00-00-00", bb.__str__())
        self.assertEqual(v, bb.read_uint())
        self.assertEqual(bb.ri, bb.wi)

        v = -1
        bb.write_uint(v)
        self.assertEqual(5, bb.size())
        self.assertEqual("F0-FF-FF-FF-FF", bb.__str__())
        self.assertEqual(v, bb.read_uint())
        self.assertEqual(bb.ri, bb.wi)

    def test_long(self):
        bb = ByteBuffer()
        self.assertEqual(bb.ri, bb.wi)

        v = 1
        bb.write_long(v)
        self.assertEqual(1, bb.size())
        self.assertEqual("01", bb.__str__())
        self.assertEqual(v, bb.read_long())
        self.assertEqual(bb.ri, bb.wi)

        v = 0x80
        bb.write_long(v)
        self.assertEqual(2, bb.size())
        self.assertEqual("40-80", bb.__str__())
        self.assertEqual(v, bb.read_long())
        self.assertEqual(bb.ri, bb.wi)

        v = 0x4000
        bb.write_long(v)
        self.assertEqual(3, bb.size())
        self.assertEqual("60-40-00", bb.__str__())
        self.assertEqual(v, bb.read_long())
        self.assertEqual(bb.ri, bb.wi)

        v = 0x20_0000
        bb.write_long(v)
        self.assertEqual(4, bb.size())
        self.assertEqual("70-20-00-00", bb.__str__())
        self.assertEqual(v, bb.read_long())
        self.assertEqual(bb.ri, bb.wi)

        v = 0x1000_0000
        bb.write_long(v)
        self.assertEqual(5, bb.size())
        self.assertEqual("78-10-00-00-00", bb.__str__())
        self.assertEqual(v, bb.read_long())
        self.assertEqual(bb.ri, bb.wi)

        v = 0x8_0000_0000
        bb.write_long(v)
        self.assertEqual(6, bb.size())
        self.assertEqual("7C-08-00-00-00-00", bb.__str__())
        self.assertEqual(v, bb.read_long())
        self.assertEqual(bb.ri, bb.wi)

        v = 0x400_0000_0000
        bb.write_long(v)
        self.assertEqual(7, bb.size())
        self.assertEqual("7E-04-00-00-00-00-00", bb.__str__())
        self.assertEqual(v, bb.read_long())
        self.assertEqual(bb.ri, bb.wi)

        v = 0x2_0000_0000_0000
        bb.write_long(v)
        self.assertEqual(8, bb.size())
        self.assertEqual("7F-02-00-00-00-00-00-00", bb.__str__())
        self.assertEqual(v, bb.read_long())
        self.assertEqual(bb.ri, bb.wi)

        v = 0x100_0000_0000_0000
        bb.write_long(v)
        self.assertEqual(9, bb.size())
        self.assertEqual("7F-81-00-00-00-00-00-00-00", bb.__str__())
        self.assertEqual(v, bb.read_long())
        self.assertEqual(bb.ri, bb.wi)

        v = -0x8000_0000_0000_0000
        bb.write_long(v)
        self.assertEqual(9, bb.size())
        self.assertEqual("80-00-00-00-00-00-00-00-00", bb.__str__())
        self.assertEqual(v, bb.read_long())
        self.assertEqual(bb.ri, bb.wi)

        v = -1
        bb.write_long(v)
        self.assertEqual(1, bb.size())
        self.assertEqual("FF", bb.__str__())
        self.assertEqual(v, bb.read_long())
        self.assertEqual(bb.ri, bb.wi)

    def check_int(self, x):
        x = ByteBuffer.truncate_int(x)
        bb = ByteBuffer()
        bb.write_int(x)
        y = bb.read_int()
        self.assertEqual(x, y)
        self.assertEqual(bb.ri, bb.wi)
        self.assertEqual(bb.wi, ByteBuffer.write_long_size(x))

    def check_long(self, x):
        x = ByteBuffer.truncate_long(x)
        bb = ByteBuffer()
        bb.write_long(x)
        y = bb.read_long()
        self.assertEqual(x, y)
        self.assertEqual(bb.ri, bb.wi)
        self.assertEqual(bb.wi, ByteBuffer.write_long_size(x))

    def check_uint(self, x):
        x = ByteBuffer.truncate_int(x)
        bb = ByteBuffer()
        bb1 = ByteBuffer()
        bb.write_uint(x)
        bb1.write_ulong(x & 0xffff_ffff)
        self.assertTrue(bb.__eq__(bb1))
        y = bb.read_uint()
        self.assertEqual(x, y)
        self.assertEqual(bb.ri, bb.wi)
        self.assertEqual(bb.wi, ByteBuffer.write_uint_size(x))
        self.assertEqual(bb.wi, ByteBuffer.write_ulong_size(x & 0xffff_ffff))

    def check_ulong(self, x):
        x = ByteBuffer.truncate_long(x)
        bb = ByteBuffer()
        bb.write_ulong(x)
        y = bb.read_ulong()
        self.assertEqual(x, y)
        self.assertEqual(bb.ri, bb.wi)
        self.assertEqual(bb.wi, ByteBuffer.write_ulong_size(x))

    def check_skip_uint(self, x):
        x = ByteBuffer.truncate_int(x)
        bb = ByteBuffer()
        bb.write_uint(x)
        bb.read_uint()
        ri = bb.ri
        bb.ri = 0
        bb.skip_uint()
        self.assertEqual(ri, bb.ri)

    def check_skip_long(self, x):
        x = ByteBuffer.truncate_long(x)
        bb = ByteBuffer()
        bb.write_long(x)
        bb.read_long()
        ri = bb.ri
        bb.ri = 0
        bb.skip_long()
        self.assertEqual(ri, bb.ri)

    def check_skip_ulong(self, x):
        x = ByteBuffer.truncate_long(x)
        bb = ByteBuffer()
        bb.write_ulong(x)
        bb.read_ulong()
        ri = bb.ri
        bb.ri = 0
        bb.skip_ulong()
        self.assertEqual(ri, bb.ri)

    def check_all(self, x):
        self.check_int(ByteBuffer.truncate_int(x))
        self.check_int(ByteBuffer.truncate_int(-x))
        self.check_uint(ByteBuffer.truncate_int(x))
        self.check_uint(ByteBuffer.truncate_int(-x))
        self.check_skip_uint(ByteBuffer.truncate_int(x))
        self.check_skip_uint(ByteBuffer.truncate_int(-x))
        self.check_long(x)
        self.check_long(-x)
        self.check_ulong(x)
        self.check_ulong(-x)
        self.check_skip_long(x)
        self.check_skip_long(-x)
        self.check_skip_ulong(x)
        self.check_skip_ulong(-x)

    def test_integer(self):
        for i in range(0, 65):
            if i < 64:
                self.check_all(1 << i)
            self.check_all((1 << i) - 1)
            self.check_all(((1 << i) - 1) & 0x5555_5555_5555_5555)
            self.check_all(((1 << i) - 1) & 0xaaaa_aaaa_aaaa_aaaa)
        int_min = -0x8000_0000
        int_max = 0x7fff_ffff
        long_min = -0x8000_0000_0000_0000
        long_max = 0x7fff_ffff_ffff_ffff
        for i in range(0, 1000):
            self.check_all(random.randint(long_min, long_max))
        self.check_int(int_min)
        self.check_int(int_max)
        self.check_long(int_min)
        self.check_long(int_max)
        self.check_long(long_min)
        self.check_long(long_max)
        self.check_uint(int_min)
        self.check_uint(int_max)
        self.check_ulong(int_min)
        self.check_ulong(int_max)
        self.check_ulong(long_min)
        self.check_ulong(long_max)
        self.check_skip_long(int_min)
        self.check_skip_long(int_max)
        self.check_skip_long(long_min)
        self.check_skip_long(long_max)
        self.check_skip_uint(int_min)
        self.check_skip_uint(int_max)
        self.check_skip_ulong(int_min)
        self.check_skip_ulong(int_max)
        self.check_skip_ulong(long_min)
        self.check_skip_ulong(long_max)

    def test_to_long(self):
        b = bytearray(8)
        vbe = 0
        vle = 0
        for n in range(1, 9):
            b[n - 1] = n
            vbe = (vbe << 8) + n
            vle += n << ((n - 1) * 8)
            self.assertEqual(vbe, ByteBuffer.to_long_be_n(b, 0, n))
            self.assertEqual(vle, ByteBuffer.to_long_n(b, 0, n))

    def test_bean(self):
        v = BValue()
        v.string3 = "abc"
        v.bytes8 = "xyz".encode('utf-8')
        bean1 = Bean1(123)
        bean1.V2[12] = 34
        v.list9.append(bean1)
        simple = BSimple()
        simple.removed.int_1 = 999
        v.map16[Key(11, "")] = simple

        bb = ByteBuffer()
        v.encode(bb)
        v2 = BValue()
        v2.decode(bb)
        bb.ri = 0
        bb2 = ByteBuffer()
        v2.encode(bb2)

        # print(v);
        # print(v2);

        self.assertEqual(bb.size(), bb2.size())
        self.assertEqual(bb, bb2)


if __name__ == '__main__':
    unittest.main()
