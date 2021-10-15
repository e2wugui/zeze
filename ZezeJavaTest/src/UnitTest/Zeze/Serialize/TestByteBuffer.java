package UnitTest.Zeze.Serialize;

import org.junit.Test;

import Zeze.Serialize.*;
import Zeze.Util.BitConverter;
import junit.framework.TestCase;


public class TestByteBuffer extends TestCase {
	public void testBytes() {
		ByteBuffer bb = ByteBuffer.Allocate();
		byte[] v = new byte[0];
		bb.WriteBytes(v);
		assertTrue(1 == bb.Size());
		assertTrue("00".equals(bb.toString()));
		assertTrue(BitConverter.toString(v).equals(BitConverter.toString(bb.ReadBytes())));
		assertTrue(bb.ReadIndex == bb.WriteIndex);

		v = new byte[]{1, 2};
		bb.WriteBytes(v);
		assertTrue(3 == bb.Size());
		assertTrue("02-01-02".equals(bb.toString()));
		assertTrue(BitConverter.toString(v).equals(BitConverter.toString(bb.ReadBytes())));
		assertTrue(bb.ReadIndex == bb.WriteIndex);
	}
	
	public void testBasic() {
		ByteBuffer bb = ByteBuffer.Allocate();
		assert bb.ReadIndex == bb.WriteIndex;

		{
			boolean v = true;
			bb.WriteBool(v);
			assert 1 == bb.Size();
			assert 1 == bb.Bytes[bb.ReadIndex];
			assert v == bb.ReadBool();
			assert bb.ReadIndex == bb.WriteIndex;
		} {
			byte v = 1;
			bb.WriteByte(v);
			assert 1 == bb.Size();
			assert 1 == bb.Bytes[bb.ReadIndex];
			assert v == bb.ReadByte();
			assert bb.ReadIndex == bb.WriteIndex;
		} {
			double v = 1.1;
			bb.WriteDouble(v);
			assert 8 == bb.Size();
			assert "3F-F1-99-99-99-99-99-9A".equals(bb.toString());
			assert v == bb.ReadDouble();
			assert bb.ReadIndex == bb.WriteIndex;
		} {
			float v = 1.1f;
			bb.WriteFloat(v);
			assert 4 == bb.Size();
			assert "3F-8C-CC-CD".equals(bb.toString());
			assert v == bb.ReadFloat();
			assert bb.ReadIndex == bb.WriteIndex;
		} {
			int int4 = 0x12345678;
			bb.WriteInt4(int4);
			assert 4 == bb.Size();
			assert "12-34-56-78".equals(bb.toString());
			assert int4 == bb.ReadInt4();
			assert bb.ReadIndex == bb.WriteIndex;
		} {
			long long8 = 0x1234567801020304L;
			bb.WriteLong8(long8);
			assert 8 == bb.Size();
			assert "12-34-56-78-01-02-03-04".equals(bb.toString());
			assert long8 == bb.ReadLong8();
			assert bb.ReadIndex == bb.WriteIndex;
		} {
			long long8 = -12345678;
			bb.WriteLong8(long8);
			assert 8 == bb.Size();
			assert "FF-FF-FF-FF-FF-43-9E-B2".equals(bb.toString());
			assert long8 == bb.ReadLong8();
			assert bb.ReadIndex == bb.WriteIndex;
		} {
			long long8 = -1;
			bb.WriteLong8(long8);
			assert 8 == bb.Size();
			assert "FF-FF-FF-FF-FF-FF-FF-FF".equals(bb.toString());
			assert long8 == bb.ReadLong8();
			assert bb.ReadIndex == bb.WriteIndex;
		}
	}

	public final void testShort() {
		ByteBuffer bb = ByteBuffer.Allocate();
		assert bb.ReadIndex == bb.WriteIndex;

		short v = 1;
		bb.WriteShort(v);
		assert 1 == bb.Size();
		assert "01".equals(bb.toString());
		assert v == bb.ReadShort();
		assert bb.ReadIndex == bb.WriteIndex;

		v = 0x80;
		bb.WriteShort(v);
		assert 2 == bb.Size();
		assert "80-80".equals(bb.toString());
		assert v == bb.ReadShort();
		assert bb.ReadIndex == bb.WriteIndex;

		v = 0x4000;
		bb.WriteShort(v);
		assert 3 == bb.Size();
		assert "FF-40-00".equals(bb.toString());
		assert v == bb.ReadShort();
		assert bb.ReadIndex == bb.WriteIndex;

		v = -1;
		bb.WriteShort(v);
		assert 3 == bb.Size();
		assert "FF-FF-FF".equals(bb.toString());
		assert v == bb.ReadShort();
		assert bb.ReadIndex == bb.WriteIndex;
	}

	public final void testInt() {
		ByteBuffer bb = ByteBuffer.Allocate();
		assert bb.ReadIndex == bb.WriteIndex;

		int v = 1;
		bb.WriteInt(v);
		assert 1 == bb.Size();
		assert "01".equals(bb.toString());
		assert v == bb.ReadInt();
		assert bb.ReadIndex == bb.WriteIndex;

		v = 0x80;
		bb.WriteInt(v);
		assert 2 == bb.Size();
		assert "80-80".equals(bb.toString());
		assert v == bb.ReadInt();
		assert bb.ReadIndex == bb.WriteIndex;

		v = 0x4000;
		bb.WriteInt(v);
		assert 3 == bb.Size();
		assert "C0-40-00".equals(bb.toString());
		assert v == bb.ReadInt();
		assert bb.ReadIndex == bb.WriteIndex;

		v = 0x200000;
		bb.WriteInt(v);
		assert 4 == bb.Size();
		assert "E0-20-00-00".equals(bb.toString());
		assert v == bb.ReadInt();
		assert bb.ReadIndex == bb.WriteIndex;

		v = 0x10000000;
		bb.WriteInt(v);
		assert 5 == bb.Size();
		assert "F0-10-00-00-00".equals(bb.toString());
		assert v == bb.ReadInt();
		assert bb.ReadIndex == bb.WriteIndex;
	}

	public final void testLong() {
		ByteBuffer bb = ByteBuffer.Allocate();
		assert bb.ReadIndex == bb.WriteIndex;

		long v = 1;
		bb.WriteLong(v);
		assert 1 == bb.Size();
		assert "01".equals(bb.toString());
		assert v == bb.ReadLong();
		assert bb.ReadIndex == bb.WriteIndex;

		v = 0x80L;
		bb.WriteLong(v);
		assert 2 == bb.Size();
		assert "80-80".equals(bb.toString());
		assert v == bb.ReadLong();
		assert bb.ReadIndex == bb.WriteIndex;

		v = 0x4000L;
		bb.WriteLong(v);
		assert 3 == bb.Size();
		assert "C0-40-00".equals(bb.toString());
		assert v == bb.ReadLong();
		assert bb.ReadIndex == bb.WriteIndex;

		v = 0x200000L;
		bb.WriteLong(v);
		assert 4 == bb.Size();
		assert "E0-20-00-00".equals(bb.toString());
		assert v == bb.ReadLong();
		assert bb.ReadIndex == bb.WriteIndex;

		v = 0x10000000L;
		bb.WriteLong(v);
		assert 5 == bb.Size();
		assert "F0-10-00-00-00".equals(bb.toString());
		assert v == bb.ReadLong();
		assert bb.ReadIndex == bb.WriteIndex;

		v = 0x800000000L;
		bb.WriteLong(v);
		assert 6 == bb.Size();
		assert "F8-08-00-00-00-00".equals(bb.toString());
		assert v == bb.ReadLong();
		assert bb.ReadIndex == bb.WriteIndex;

		v = 0x40000000000L;
		bb.WriteLong(v);
		assert 7 == bb.Size();
		assert "FC-04-00-00-00-00-00".equals(bb.toString());
		assert v == bb.ReadLong();
		assert bb.ReadIndex == bb.WriteIndex;

		v = 0x2000000000000L;
		bb.WriteLong(v);
		assert 8 == bb.Size();
		assert "FE-02-00-00-00-00-00-00".equals(bb.toString());
		assert v == bb.ReadLong();
		assert bb.ReadIndex == bb.WriteIndex;

		v = 0x100000000000000L;
		bb.WriteLong(v);
		assert 9 == bb.Size();
		
		assert "FF-01-00-00-00-00-00-00-00".equals(bb.toString());
		assert v == bb.ReadLong();
		assert bb.ReadIndex == bb.WriteIndex;

		v = 0L;
		v = (long)((long)v | 0x8000000000000000L);
		bb.WriteLong(v);
		assert 9 == bb.Size();
		assert "FF-80-00-00-00-00-00-00-00".equals(bb.toString());
		assert v == bb.ReadLong();
		assert bb.ReadIndex == bb.WriteIndex;
	}
}
