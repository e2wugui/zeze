
import { Zeze } from "./zeze.js"
import Long from "./long.js"

function assert(condition: any, msg: string): asserts condition {
    if (!condition) {
        throw new Error(msg);
    }
}

function test(x: string): string {
    {
        var bb = new Zeze.ByteBuffer();
        var v = new Uint8Array(0);
        bb.WriteBytes(v);
        assert(bb.Size() == 1, "assert 1");
        assert(bb.toString() == "00", "assert 2");
        assert(Zeze.ByteBuffer.toString(v) == Zeze.ByteBuffer.toString(bb.ReadBytes()), "assert 3");
        assert(bb.ReadIndex == bb.WriteIndex, "assert 4");

        v = new Uint8Array(2);
        v[0] = 1; v[1] = 2;
        bb.WriteBytes(v);
        assert(bb.Size() == 3, "assert 5");
        assert(bb.toString() == "02-01-02", "assert 6");
        assert(Zeze.ByteBuffer.toString(v) == Zeze.ByteBuffer.toString(bb.ReadBytes()), "assert 7");
        assert(bb.ReadIndex == bb.WriteIndex, "assert 8");
    }
    {
        var bb = new Zeze.ByteBuffer();
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
        var bb = new Zeze.ByteBuffer();
        assert(bb.ReadIndex == bb.WriteIndex, "assert 26");

        var shortv = 1;
        bb.WriteShort(shortv);
        assert(1 == bb.Size(), "assert 27");
        assert("01" == bb.toString(), "assert 28");
        assert(shortv == bb.ReadShort(), "assert 29");
        assert(bb.ReadIndex == bb.WriteIndex, "assert 30");

        shortv = 0x80;
        bb.WriteShort(shortv);
        assert(2 == bb.Size(), "assert 31");
        assert("80-80" == bb.toString(), "assert 32");
        assert(shortv == bb.ReadShort(), "assert 33");
        assert(bb.ReadIndex == bb.WriteIndex, "assert 34");

        shortv = 0x4000;
        bb.WriteShort(shortv);
        assert(3 == bb.Size(), "assert 35");
        assert("FF-40-00" == bb.toString(), "assert 36");
        assert(shortv == bb.ReadShort(), "assert 37");
        assert(bb.ReadIndex == bb.WriteIndex, "assert 38");

        shortv = -1;
        bb.WriteShort(shortv);
        assert(3 == bb.Size(), "assert 39");
        assert("FF-FF-FF" == bb.toString(), "assert 40");
        assert(shortv == bb.ReadShort(), "assert 41");
        assert(bb.ReadIndex == bb.WriteIndex, "assert 42");
    }
    {
        var bb = new Zeze.ByteBuffer();
        assert(bb.ReadIndex == bb.WriteIndex, "assert 43");

        var int4 = 0x1234;
        bb.WriteInt4(int4);
        assert(4 == bb.Size(), "assert 44");
        assert("34-12-00-00" == bb.toString(), "assert 45");
        assert(int4 == bb.ReadInt4(), "assert 46");
        assert(bb.ReadIndex == bb.WriteIndex, "assert 47");
    }

    {
        var bb = new Zeze.ByteBuffer();
        assert(bb.ReadIndex == bb.WriteIndex, "assert 48");

        var long8 = new Long(0x5678, 0x1234, true);
        bb.WriteLong8(long8);
        assert(8 == bb.Size(), "assert 49");
        assert("78-56-00-00-34-12-00-00" == bb.toString(), "assert 50");
        assert(long8.equals(bb.ReadLong8()), "assert 51");
        assert(bb.ReadIndex == bb.WriteIndex, "assert 52");
    }
    {
        var bb = new Zeze.ByteBuffer();
        assert(bb.ReadIndex == bb.WriteIndex, "assert 53");

        var intv = 1;
        bb.WriteInt(intv);
        assert(1 == bb.Size(), "assert 54");
        assert("01" == bb.toString(), "assert 55");
        assert(intv == bb.ReadInt(), "assert 56");
        assert(bb.ReadIndex == bb.WriteIndex, "assert 57");

        intv = 0x80;
        bb.WriteInt(intv);
        assert(2 == bb.Size(), "assert 58");
        assert("80-80" == bb.toString(), "assert 59");
        assert(intv == bb.ReadInt(), "assert 60");
        assert(bb.ReadIndex == bb.WriteIndex, "assert 61");

        intv = 0x4000;
        bb.WriteInt(intv);
        assert(3 == bb.Size(), "assert 62");
        assert("C0-40-00" == bb.toString(), "assert 63");
        assert(intv == bb.ReadInt(), "assert 64");
        assert(bb.ReadIndex == bb.WriteIndex, "assert 65");

        intv = 0x200000;
        bb.WriteInt(intv);
        assert(4 == bb.Size(), "assert 66");
        assert("E0-20-00-00" == bb.toString(), "assert 67");
        assert(intv == bb.ReadInt(), "assert 68");
        assert(bb.ReadIndex == bb.WriteIndex, "assert 69");

        intv = 0x10000000;
        bb.WriteInt(intv);
        assert(5 == bb.Size(), "assert 70");
        assert("F0-10-00-00-00" == bb.toString(), "assert 71");
        assert(intv == bb.ReadInt(), "assert 72");
        assert(bb.ReadIndex == bb.WriteIndex, "assert 73");

        intv = -1;
        bb.WriteInt(intv);
        assert(5 == bb.Size(), "assert 74");
        assert("F0-FF-FF-FF-FF" == bb.toString(), "assert 75");
        assert(intv == bb.ReadInt(), "assert 76");
        assert(bb.ReadIndex == bb.WriteIndex, "assert 77");
    }
    {
        var bb = new Zeze.ByteBuffer();
        assert(bb.ReadIndex == bb.WriteIndex, "assert 78");

        var longv = new Long(1, 0, true);
        bb.WriteLong(longv);
        assert(1 == bb.Size(), "assert 79");
        assert("01" == bb.toString(), "assert 80");
        assert(longv.eq(bb.ReadLong()), "assert 81");
        assert(bb.ReadIndex == bb.WriteIndex, "assert 82");

        longv = new Long(0x80, 0, true);
        bb.WriteLong(longv);
        assert(2 == bb.Size(), "assert 83");
        assert("80-80" == bb.toString(), "assert 84");
        assert(longv.eq(bb.ReadLong()), "assert 85");
        assert(bb.ReadIndex == bb.WriteIndex, "assert 86");

        longv = new Long(0x4000, 0, true);
        bb.WriteLong(longv);
        assert(3 == bb.Size(), "assert 87");
        assert("C0-40-00" == bb.toString(), "assert 88");
        assert(longv.eq(bb.ReadLong()), "assert 89");
        assert(bb.ReadIndex == bb.WriteIndex, "assert 90");

        longv = new Long(0x200000, 0, true);
        bb.WriteLong(longv);
        assert(4 == bb.Size(), "assert 91");
        assert("E0-20-00-00" == bb.toString(), "assert 92");
        assert(longv.eq(bb.ReadLong()), "assert 93");
        assert(bb.ReadIndex == bb.WriteIndex, "assert 94");

        longv = new Long(0x10000000, 0, true);
        bb.WriteLong(longv);
        assert(5 == bb.Size(), "assert 95");
        assert("F0-10-00-00-00" == bb.toString(), "assert 96");
        assert(longv.eq(bb.ReadLong()), "assert 97");
        assert(bb.ReadIndex == bb.WriteIndex, "assert 98");

        longv = new Long(0, 0x8, true);
        bb.WriteLong(longv);
        assert(6 == bb.Size(), "assert 99");
        //Console.WriteLine(bb);
        assert("F8-08-00-00-00-00" == bb.toString(), "assert 100");
        assert(longv.eq(bb.ReadLong()), "assert 101");
        assert(bb.ReadIndex == bb.WriteIndex, "assert 102");

        longv = new Long(0, 0x400, true);
        bb.WriteLong(longv);
        assert(7 == bb.Size(), "assert 103");
        assert("FC-04-00-00-00-00-00" == bb.toString(), "assert 104");
        assert(longv.eq(bb.ReadLong()), "assert 105");
        assert(bb.ReadIndex == bb.WriteIndex, "assert 106");

        longv = new Long(0, 0x2000, true);
        bb.WriteLong(longv);
        assert(8 == bb.Size(), "assert 107");
        assert("FE-00-20-00-00-00-00-00" == bb.toString(), "assert 108");
        assert(longv.eq(bb.ReadLong()), "assert 109");
        assert(bb.ReadIndex == bb.WriteIndex, "assert 110");

        longv = new Long(0, 0x1000000, true);
        bb.WriteLong(longv);
        assert(9 == bb.Size(), "assert 111");
        assert("FF-01-00-00-00-00-00-00-00" == bb.toString(), "assert 112");
        assert(longv.eq(bb.ReadLong()), "assert 113");
        assert(bb.ReadIndex == bb.WriteIndex, "assert 114");

        longv = new Long(-1, 0, true);
        bb.WriteLong(longv);
        assert(5 == bb.Size(), "assert 115");
        assert("F0-FF-FF-FF-FF" == bb.toString(), "assert 116");
        assert(longv.eq(bb.ReadLong()), "assert 117");
        assert(bb.ReadIndex == bb.WriteIndex, "assert 118");

        longv = new Long(-1, -1, true);
        bb.WriteLong(longv);
        assert(9 == bb.Size(), "assert 119");
        assert("FF-FF-FF-FF-FF-FF-FF-FF-FF" == bb.toString(), "assert 120");
        assert(longv.eq(bb.ReadLong()), "assert 121");
        assert(bb.ReadIndex == bb.WriteIndex, "assert 122");
    }
    return "ok";
}

window.globalThis.test = test;
