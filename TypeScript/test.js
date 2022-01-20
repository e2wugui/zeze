"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Test = void 0;
const zeze_1 = require("zeze");
function assert(condition, msg) {
    if (!condition) {
        throw new Error(msg);
    }
}
class Test {
    constructor(p) {
        this.p = p;
    }
    test() {
        {
            var bb = new zeze_1.Zeze.ByteBuffer();
            var v = new Uint8Array(0);
            bb.WriteBytes(v);
            assert(bb.Size() == 1, "assert 1");
            assert(bb.toString() == "00", "assert 2");
            assert(zeze_1.Zeze.ByteBuffer.toString(v) == zeze_1.Zeze.ByteBuffer.toString(bb.ReadBytes()), "assert 3");
            assert(bb.ReadIndex == bb.WriteIndex, "assert 4");
            v = new Uint8Array(2);
            v[0] = 1;
            v[1] = 2;
            bb.WriteBytes(v);
            assert(bb.Size() == 3, "assert 5");
            assert(bb.toString() == "02-01-02", "assert 6");
            assert(zeze_1.Zeze.ByteBuffer.toString(v) == zeze_1.Zeze.ByteBuffer.toString(bb.ReadBytes()), "assert 7");
            assert(bb.ReadIndex == bb.WriteIndex, "assert 8");
        }
        {
            var bb = new zeze_1.Zeze.ByteBuffer();
            assert(bb.ReadIndex == bb.WriteIndex, "assert 9");
            {
                var b = true;
                bb.WriteBool(b);
                assert(1 == bb.Size(), "assert 10");
                assert(1 == bb.Bytes[bb.ReadIndex], "assert 11");
                assert(b == bb.ReadBool(), "assert 12");
                assert(bb.ReadIndex == bb.WriteIndex, "assert 13");
            }
            {
                var vbyte = 1;
                bb.WriteByte(vbyte);
                assert(1 == bb.Size(), "assert 14");
                assert(1 == bb.Bytes[bb.ReadIndex], "assert 15");
                assert(vbyte == bb.ReadByte(), "assert 16");
                assert(bb.ReadIndex == bb.WriteIndex, "assert 17");
            }
            {
                var vdouble = 1.1;
                bb.WriteDouble(vdouble);
                assert(8 == bb.Size(), "assert 18");
                assert("9A-99-99-99-99-99-F1-3F" == bb.toString(), "assert 19");
                assert(vdouble == bb.ReadDouble(), "assert 20");
                assert(bb.ReadIndex == bb.WriteIndex, "assert 21");
            }
            {
                var vfloat = 1.1;
                bb.WriteFloat(vfloat);
                assert(4 == bb.Size(), "assert 22");
                assert("CD-CC-8C-3F" == bb.toString(), "assert 23");
                assert(Math.abs(vfloat - bb.ReadFloat()) < 0.00001, "assert 24"); // XXX
                assert(bb.ReadIndex == bb.WriteIndex, "assert 25");
            }
        }
        {
            var bb = new zeze_1.Zeze.ByteBuffer();
            assert(bb.ReadIndex == bb.WriteIndex, "assert 43");
            var int4 = 0x1234;
            bb.WriteInt4(int4);
            assert(4 == bb.Size(), "assert 44");
            assert("34-12-00-00" == bb.toString(), "assert 45");
            assert(int4 == bb.ReadInt4(), "assert 46");
            assert(bb.ReadIndex == bb.WriteIndex, "assert 47");
        }
        {
            var bb = new zeze_1.Zeze.ByteBuffer();
            assert(bb.ReadIndex == bb.WriteIndex, "assert 48");
            var long8 = 0x1234567801020304n;
            bb.WriteLong8(long8);
            assert(8 == bb.Size(), "assert 49");
            assert("04-03-02-01-78-56-34-12" == bb.toString(), "assert 50");
            assert(long8 == bb.ReadLong8(), "assert 51");
            assert(bb.ReadIndex == bb.WriteIndex, "assert 52");
        }
        {
            var bb = new zeze_1.Zeze.ByteBuffer();
            assert(bb.ReadIndex == bb.WriteIndex, "assert 48_");
            var long8 = -12345678n;
            bb.WriteLong8(long8);
            assert(8 == bb.Size(), "assert 49_");
            assert("B2-9E-43-FF-FF-FF-FF-FF" == bb.toString(), "assert 50_");
            var readlong8 = bb.ReadLong8();
            //console.log(readlong8);
            assert(long8 == readlong8, "assert 51_");
            assert(bb.ReadIndex == bb.WriteIndex, "assert 52_");
        }
        {
            var bb = new zeze_1.Zeze.ByteBuffer();
            assert(bb.ReadIndex == bb.WriteIndex, "assert 53");
            var intv = 1;
            bb.WriteUInt(intv);
            assert(1 == bb.Size(), "assert 54");
            assert("01" == bb.toString(), "assert 55");
            assert(intv == bb.ReadUInt(), "assert 56");
            assert(bb.ReadIndex == bb.WriteIndex, "assert 57");
            intv = 0x80;
            bb.WriteUInt(intv);
            assert(2 == bb.Size(), "assert 58");
            assert("80-80" == bb.toString(), "assert 59");
            assert(intv == bb.ReadUInt(), "assert 60");
            assert(bb.ReadIndex == bb.WriteIndex, "assert 61");
            intv = 0x4000;
            bb.WriteUInt(intv);
            assert(3 == bb.Size(), "assert 62");
            assert("C0-40-00" == bb.toString(), "assert 63");
            assert(intv == bb.ReadUInt(), "assert 64");
            assert(bb.ReadIndex == bb.WriteIndex, "assert 65");
            intv = 2097152;
            bb.WriteUInt(intv);
            assert(4 == bb.Size(), "assert 66");
            assert("E0-20-00-00" == bb.toString(), "assert 67");
            assert(intv == bb.ReadUInt(), "assert 68");
            assert(bb.ReadIndex == bb.WriteIndex, "assert 69");
            intv = 268435456;
            bb.WriteUInt(intv);
            assert(5 == bb.Size(), "assert 70");
            assert("F0-10-00-00-00" == bb.toString(), "assert 71");
            assert(intv == bb.ReadUInt(), "assert 72");
            assert(bb.ReadIndex == bb.WriteIndex, "assert 73");
            intv = -1;
            bb.WriteUInt(intv);
            assert(5 == bb.Size(), "assert 74");
            assert("F0-FF-FF-FF-FF" == bb.toString(), "assert 75");
            assert(intv == bb.ReadUInt(), "assert 76");
            assert(bb.ReadIndex == bb.WriteIndex, "assert 77");
        }
        {
            var bb = new zeze_1.Zeze.ByteBuffer();
            assert(bb.ReadIndex == bb.WriteIndex, "assert 78");
            var longv = 1n;
            bb.WriteLong(longv);
            assert(1 == bb.Size(), "assert 79");
            assert("01" == bb.toString(), "assert 80");
            assert(longv == bb.ReadLong(), "assert 81");
            assert(bb.ReadIndex == bb.WriteIndex, "assert 82");
            longv = 0x80n;
            bb.WriteLong(longv);
            assert(2 == bb.Size(), "assert 83");
            assert("40-80" == bb.toString(), "assert 84");
            assert(longv == bb.ReadLong(), "assert 85");
            assert(bb.ReadIndex == bb.WriteIndex, "assert 86");
            longv = 0x4000n;
            bb.WriteLong(longv);
            assert(3 == bb.Size(), "assert 87");
            assert("60-40-00" == bb.toString(), "assert 88");
            assert(longv == bb.ReadLong(), "assert 89");
            assert(bb.ReadIndex == bb.WriteIndex, "assert 90");
            longv = 0x200000n;
            bb.WriteLong(longv);
            assert(4 == bb.Size(), "assert 91");
            assert("70-20-00-00" == bb.toString(), "assert 92");
            assert(longv == bb.ReadLong(), "assert 93");
            assert(bb.ReadIndex == bb.WriteIndex, "assert 94");
            longv = 0x10000000n;
            bb.WriteLong(longv);
            assert(5 == bb.Size(), "assert 95");
            assert("78-10-00-00-00" == bb.toString(), "assert 96");
            assert(longv == bb.ReadLong(), "assert 97");
            assert(bb.ReadIndex == bb.WriteIndex, "assert 98");
            longv = 0x800000000n; //new Long(0, 0x8, true);
            bb.WriteLong(longv);
            assert(6 == bb.Size(), "assert 99");
            //Console.WriteLine(bb);
            assert("7C-08-00-00-00-00" == bb.toString(), "assert 100");
            assert(longv == bb.ReadLong(), "assert 101");
            assert(bb.ReadIndex == bb.WriteIndex, "assert 102");
            longv = 0x40000000000n; //new Long(0, 0x400, true);
            bb.WriteLong(longv);
            assert(7 == bb.Size(), "assert 103");
            assert("7E-04-00-00-00-00-00" == bb.toString(), "assert 104");
            assert(longv == bb.ReadLong(), "assert 105");
            assert(bb.ReadIndex == bb.WriteIndex, "assert 106");
            longv = 0x2000000000000n; //new Long(0, 0x20000, true);
            bb.WriteLong(longv);
            assert(8 == bb.Size(), "assert 107");
            assert("7F-02-00-00-00-00-00-00" == bb.toString(), "assert 108");
            assert(longv == bb.ReadLong(), "assert 109");
            assert(bb.ReadIndex == bb.WriteIndex, "assert 110");
            longv = 0x100000000000000n; // new Long(0, 0x1000000, true);
            bb.WriteLong(longv);
            assert(9 == bb.Size(), "assert 111");
            //console.log(bb.toString());
            assert("7F-81-00-00-00-00-00-00-00" == bb.toString(), "assert 112");
            assert(longv == bb.ReadLong(), "assert 113");
            assert(bb.ReadIndex == bb.WriteIndex, "assert 114");
            longv = -9223372036854775808n; // 0x8000_0000_0000_0000n new Long(0, 0x1000000, true);
            //console.log("---" + longv.toString(16));
            bb.WriteLong(longv);
            assert(9 == bb.Size(), "assert 115");
            assert("80-00-00-00-00-00-00-00-00" == bb.toString(), "assert 116");
            assert(longv == bb.ReadLong(), "assert 117");
            assert(bb.ReadIndex == bb.WriteIndex, "assert 118");
            longv = -1n; // new Long(-1, -1, true);
            bb.WriteLong(longv);
            assert(1 == bb.Size(), "assert 119");
            assert("FF" == bb.toString(), "assert 120");
            assert(longv == bb.ReadLong(), "assert 121");
            assert(bb.ReadIndex == bb.WriteIndex, "assert 122");
        }
        {
            bb.WriteInt(-268435456);
            var b1 = bb.ReadByte() - 0x100;
            for (var i = 0; i <= 64; ++i) {
                Test.testAll(1n << BigInt(i));
                Test.testAll((1n << BigInt(i)) - 1n);
                Test.testAll(((1n << BigInt(i)) - 1n) & 0x5555555555555555n);
                Test.testAll(((1n << BigInt(i)) - 1n) & 0xaaaaaaaaaaaaaaaan);
            }
            Test.testInt(-2147483648);
            Test.testInt(2147483647);
            Test.testLong(-0x80000000n);
            Test.testLong(0x7fffffffn);
            Test.testLong(-0x8000000000000000n);
            Test.testLong(0x7fffffffffffffffn);
            Test.testUInt(2147483647);
        }
        return "ok";
    }
    static testInt(x) {
        if (x < -2147483648 || x > 2147483647)
            return;
        var bb = new zeze_1.Zeze.ByteBuffer();
        bb.WriteInt(x);
        var s = bb.toString();
        var y = bb.ReadInt();
        assert(x == y, "testInt 1: " + s + ": " + x + " != " + y);
        assert(bb.ReadIndex == bb.WriteIndex, "testInt 2: " + x);
    }
    static testLong(x) {
        if (x < -0x8000000000000000n || x > 0x7fffffffffffffffn)
            return;
        var bb = new zeze_1.Zeze.ByteBuffer();
        bb.WriteLong(x);
        var s = bb.toString();
        var y = bb.ReadLong();
        assert(x == y, "testLong 1: " + s + ": " + x + " != " + y);
        assert(bb.ReadIndex == bb.WriteIndex, "testLong 2: " + x);
    }
    static testUInt(x) {
        if (x < -2147483648 || x > 2147483647)
            return;
        var bb = new zeze_1.Zeze.ByteBuffer();
        bb.WriteUInt(x);
        var s = bb.toString();
        var y = bb.ReadUInt();
        assert(x == y, "testUInt 1: " + s + ": " + x + " != " + y);
        assert(bb.ReadIndex == bb.WriteIndex, "testUInt 2: " + x);
    }
    static testAll(x) {
        Test.testInt(Number(x));
        Test.testInt(Number(-x));
        Test.testUInt(Number(x));
        Test.testUInt(Number(-x));
        Test.testLong(x);
        Test.testLong(-x);
    }
}
exports.Test = Test;
//# sourceMappingURL=test.js.map