import unittest
import zeze.util as util


class TestByteBuffer(unittest.TestCase):
    def test_util(self):
        bs = bytearray(256)
        for i in range(0, len(bs)):
            bs[i] = i
        self.assertEqual("00-01-02-03-04-05-06-07-08-09", util.to_string_with_limit(10, bs, 0, 10))
        self.assertEqual("00-01-02-03-04-05-06-07-08-09...[+246]", util.to_string_with_limit(10, bs))
        self.assertEqual("00-01-02-03-04-05...[+246]...FC-FD-FE-FF", util.to_string_with_limit2(6, 4, bs))
        self.assertEqual("00-01-02-03-04-05-06-07-08-09", util.to_string_with_limit2(6, 4, bs, 0, 10))
        self.assertEqual("00-01-02-03-04-05...[+1]...07-08-09-0A", util.to_string_with_limit2(6, 4, bs, 0, 11))
        self.assertEqual("00-01-02-03-04-05...[+4]", util.to_string_with_limit2(6, 0, bs, 0, 10))
        self.assertEqual("[+6]...06-07-08-09", util.to_string_with_limit2(0, 4, bs, 0, 10))
        self.assertEqual("[+10]", util.to_string_with_limit2(0, 0, bs, 0, 10))

        s = "01-23-45-67-89-ab-cd-ef-AB-CD-EF"
        self.assertEqual(s.upper(), util.to_string(util.to_bytes(s)))


if __name__ == '__main__':
    unittest.main()
