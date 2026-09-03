package UnitTest.Zeze.Serialize;
import harness.Fast;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.NioByteBuffer;
import Zeze.Serialize.Vector2;
import Zeze.Serialize.Vector2Int;
import Zeze.Serialize.Vector3;
import Zeze.Serialize.Vector3Int;
import Zeze.Serialize.Vector4;
import Zeze.Transaction.EmptyBean;
import Zeze.Util.BitConverter;
import Zeze.Util.Random;
import demo.Bean1;
import demo.Module1.BItem;
import demo.Module1.Key;
import demo.Module1.BSimple;
import demo.Module1.BValue;
import org.junit.jupiter.api.Assertions;

@Fast
public class TestByteBuffer {
	@Test
	public void testBitConverter() {
		var bytes = new byte[256];
		for (int i = 0; i < bytes.length; ++i)
			bytes[i] = (byte)i;
		assertEquals("00-01-02-03-04-05-06-07-08-09", BitConverter.toStringWithLimit(bytes, 0, 10, 10));
		assertEquals("00-01-02-03-04-05-06-07-08-09...[+246]", BitConverter.toStringWithLimit(bytes, 10));
		assertEquals("00-01-02-03-04-05...[+246]...FC-FD-FE-FF", BitConverter.toStringWithLimit(bytes, 6, 4));
		assertEquals("00-01-02-03-04-05-06-07-08-09", BitConverter.toStringWithLimit(bytes, 0, 10, 6, 4));
		assertEquals("00-01-02-03-04-05...[+1]...07-08-09-0A", BitConverter.toStringWithLimit(bytes, 0, 11, 6, 4));
		assertEquals("00-01-02-03-04-05...[+4]", BitConverter.toStringWithLimit(bytes, 0, 10, 6, 0));
		assertEquals("[+6]...06-07-08-09", BitConverter.toStringWithLimit(bytes, 0, 10, 0, 4));
		assertEquals("[+10]", BitConverter.toStringWithLimit(bytes, 0, 10, 0, 0));

		String s = "01-23-45-67-89-ab-cd-ef-AB-CD-EF";
		assertEquals(s.toUpperCase(), BitConverter.toString(BitConverter.toBytes(s)));
	}

	@Test

	public void testBytes() {
		ByteBuffer bb = ByteBuffer.Allocate();
		byte[] v = ByteBuffer.Empty;
		bb.WriteBytes(v);
		assertEquals(1, bb.size());
		assertEquals("00", bb.toString());
		assertEquals(BitConverter.toString(v), BitConverter.toString(bb.ReadBytes()));
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		var nbb = NioByteBuffer.Wrap(bb.Bytes, bb.WriteIndex);
		assertEquals(1, nbb.size());
		// assertEquals("00", nbb.toString());
		assertEquals(BitConverter.toString(v), BitConverter.toString(nbb.ReadBytes()));
		assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());

		v = new byte[]{1, 2};
		bb.WriteBytes(v);
		nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
		assertEquals(3, bb.size());
		assertEquals("02-01-02", bb.toString());
		assertEquals(BitConverter.toString(v), BitConverter.toString(bb.ReadBytes()));
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		assertEquals(3, nbb.size());
		// assertEquals("02-01-02", nbb.toString());
		assertEquals(BitConverter.toString(v), BitConverter.toString(nbb.ReadBytes()));
		assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());

		var str = "abc汉字123";
		bb.WriteString(str);
		nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
		assertEquals(13, bb.size());
		assertEquals("0C-61-62-63-E6-B1-89-E5-AD-97-31-32-33", bb.toString());
		assertEquals(str, bb.ReadString());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		assertEquals(13, nbb.size());
		assertEquals(str, nbb.ReadString());
		assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());

		str = new String(new char[]{0xd800 + 0x155, 0xdc00 + 0x2aa, 0xd800 + 0x2aa, 0xdc00 + 0x155}); // surrogate chars
		byte[] b0 = str.getBytes(StandardCharsets.UTF_8);
		bb.Reset();
		bb.WriteString(str);
		assertEquals(4 * 2, b0.length);
		assertEquals(4 * 2, ByteBuffer.utf8Size(str));
		assertEquals(4 * 2, bb.Bytes[0]);
		Assertions.assertArrayEquals(b0, bb.ReadBytes());
		bb.ReadIndex = 0;
		assertEquals(str, bb.ReadString());

		nbb = NioByteBuffer.Wrap(bb.Bytes, bb.WriteIndex);
		assertEquals(str, nbb.ReadString());
	}

	@Test

	public void testBasic() {
		ByteBuffer bb = ByteBuffer.Allocate();
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		{
			boolean v = true;
			bb.WriteBool(v);
			var nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
			assertEquals(1, bb.size());
			assertEquals(1, bb.Bytes[bb.ReadIndex]);
			assertEquals(v, bb.ReadBool());
			assertEquals(bb.ReadIndex, bb.WriteIndex);

			assertEquals(1, nbb.size());
			assertEquals(v, nbb.ReadBool());
			assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());
		}
		{
			byte v = 1;
			bb.WriteByte(v);
			var nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
			assertEquals(1, bb.size());
			assertEquals(1, bb.Bytes[bb.ReadIndex]);
			assertEquals(v, bb.ReadByte());
			assertEquals(bb.ReadIndex, bb.WriteIndex);

			assertEquals(1, nbb.size());
			assertEquals(v, nbb.ReadByte());
			assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());
		}
		{
			double v = 1.1;
			bb.WriteDouble(v);
			var nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
			assertEquals(8, bb.size());
			assertEquals("9A-99-99-99-99-99-F1-3F", bb.toString());
			assertEquals(v, bb.ReadDouble());
			assertEquals(bb.ReadIndex, bb.WriteIndex);

			assertEquals(8, nbb.size());
			assertEquals(v, nbb.ReadDouble());
			assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());
		}
		{
			float v = 1.1f;
			bb.WriteFloat(v);
			var nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
			assertEquals(4, bb.size());
			assertEquals("CD-CC-8C-3F", bb.toString());
			assertEquals(v, bb.ReadFloat());
			assertEquals(bb.ReadIndex, bb.WriteIndex);

			assertEquals(4, nbb.size());
			assertEquals(v, nbb.ReadFloat());
			assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());
		}
		{
			int int4 = 0x12345678;
			bb.WriteInt4(int4);
			var nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
			assertEquals(4, bb.size());
			assertEquals("78-56-34-12", bb.toString());
			assertEquals(int4, bb.ReadInt4());
			assertEquals(bb.ReadIndex, bb.WriteIndex);

			assertEquals(4, nbb.size());
			assertEquals(int4, nbb.ReadInt4());
			assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());
		}
		{
			long long8 = 0x1234567801020304L;
			bb.WriteLong8(long8);
			var nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
			assertEquals(8, bb.size());
			assertEquals("04-03-02-01-78-56-34-12", bb.toString());
			assertEquals(long8, bb.ReadLong8());
			assertEquals(bb.ReadIndex, bb.WriteIndex);

			assertEquals(8, nbb.size());
			assertEquals(long8, nbb.ReadLong8());
			assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());
		}
		{
			long long8 = -12345678;
			bb.WriteLong8(long8);
			var nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
			assertEquals(8, bb.size());
			assertEquals("B2-9E-43-FF-FF-FF-FF-FF", bb.toString());
			assertEquals(long8, bb.ReadLong8());
			assertEquals(bb.ReadIndex, bb.WriteIndex);

			assertEquals(8, nbb.size());
			assertEquals(long8, nbb.ReadLong8());
			assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());
		}
		{
			long long8 = -1;
			bb.WriteLong8(long8);
			var nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
			assertEquals(8, bb.size());
			assertEquals("FF-FF-FF-FF-FF-FF-FF-FF", bb.toString());
			assertEquals(long8, bb.ReadLong8());
			assertEquals(bb.ReadIndex, bb.WriteIndex);

			assertEquals(8, nbb.size());
			assertEquals(long8, nbb.ReadLong8());
			assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());
		}
	}

	@Test

	public void testUInt() {
		ByteBuffer bb = ByteBuffer.Allocate();
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		int v = 1;
		bb.WriteUInt(v);
		var nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
		assertEquals(1, bb.size());
		assertEquals("01", bb.toString());
		assertEquals(v, bb.ReadUInt());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		assertEquals(1, nbb.size());
		assertEquals(v, nbb.ReadUInt());
		assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());

		v = 0x80;
		bb.WriteUInt(v);
		nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
		assertEquals(2, bb.size());
		assertEquals("80-80", bb.toString());
		assertEquals(v, bb.ReadUInt());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		assertEquals(2, nbb.size());
		assertEquals(v, nbb.ReadUInt());
		assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());

		v = 0x4000;
		bb.WriteUInt(v);
		nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
		assertEquals(3, bb.size());
		assertEquals("C0-40-00", bb.toString());
		assertEquals(v, bb.ReadUInt());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		assertEquals(3, nbb.size());
		assertEquals(v, nbb.ReadUInt());
		assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());

		v = 0x20_0000;
		bb.WriteUInt(v);
		nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
		assertEquals(4, bb.size());
		assertEquals("E0-20-00-00", bb.toString());
		assertEquals(v, bb.ReadUInt());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		assertEquals(4, nbb.size());
		assertEquals(v, nbb.ReadUInt());
		assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());

		v = 0x1000_0000;
		bb.WriteUInt(v);
		nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
		assertEquals(5, bb.size());
		assertEquals("F0-10-00-00-00", bb.toString());
		assertEquals(v, bb.ReadUInt());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		assertEquals(5, nbb.size());
		assertEquals(v, nbb.ReadUInt());
		assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());

		v = -1;
		bb.WriteUInt(v);
		nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
		assertEquals(5, bb.size());
		assertEquals("F0-FF-FF-FF-FF", bb.toString());
		assertEquals(v, bb.ReadUInt());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		assertEquals(5, nbb.size());
		assertEquals(v, nbb.ReadUInt());
		assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());
	}

	@Test

	public void testLong() {
		ByteBuffer bb = ByteBuffer.Allocate();
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		long v = 1;
		bb.WriteLong(v);
		var nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
		assertEquals(1, bb.size());
		assertEquals("01", bb.toString());
		assertEquals(v, bb.ReadLong());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		assertEquals(1, nbb.size());
		assertEquals(v, nbb.ReadLong());
		assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());

		v = 0x80L;
		bb.WriteLong(v);
		nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
		assertEquals(2, bb.size());
		assertEquals("40-80", bb.toString());
		assertEquals(v, bb.ReadLong());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		assertEquals(2, nbb.size());
		assertEquals(v, nbb.ReadLong());
		assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());

		v = 0x4000L;
		bb.WriteLong(v);
		nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
		assertEquals(3, bb.size());
		assertEquals("60-40-00", bb.toString());
		assertEquals(v, bb.ReadLong());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		assertEquals(3, nbb.size());
		assertEquals(v, nbb.ReadLong());
		assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());

		v = 0x20_0000L;
		bb.WriteLong(v);
		nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
		assertEquals(4, bb.size());
		assertEquals("70-20-00-00", bb.toString());
		assertEquals(v, bb.ReadLong());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		assertEquals(4, nbb.size());
		assertEquals(v, nbb.ReadLong());
		assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());

		v = 0x1000_0000L;
		bb.WriteLong(v);
		nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
		assertEquals(5, bb.size());
		assertEquals("78-10-00-00-00", bb.toString());
		assertEquals(v, bb.ReadLong());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		assertEquals(5, nbb.size());
		assertEquals(v, nbb.ReadLong());
		assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());

		v = 0x8_0000_0000L;
		bb.WriteLong(v);
		nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
		assertEquals(6, bb.size());
		assertEquals("7C-08-00-00-00-00", bb.toString());
		assertEquals(v, bb.ReadLong());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		assertEquals(6, nbb.size());
		assertEquals(v, nbb.ReadLong());
		assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());

		v = 0x400_0000_0000L;
		bb.WriteLong(v);
		nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
		assertEquals(7, bb.size());
		assertEquals("7E-04-00-00-00-00-00", bb.toString());
		assertEquals(v, bb.ReadLong());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		assertEquals(7, nbb.size());
		assertEquals(v, nbb.ReadLong());
		assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());

		v = 0x2_0000_0000_0000L;
		bb.WriteLong(v);
		nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
		assertEquals(8, bb.size());
		assertEquals("7F-02-00-00-00-00-00-00", bb.toString());
		assertEquals(v, bb.ReadLong());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		assertEquals(8, nbb.size());
		assertEquals(v, nbb.ReadLong());
		assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());

		v = 0x100_0000_0000_0000L;
		bb.WriteLong(v);
		nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
		assertEquals(9, bb.size());
		assertEquals("7F-81-00-00-00-00-00-00-00", bb.toString());
		assertEquals(v, bb.ReadLong());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		assertEquals(9, nbb.size());
		assertEquals(v, nbb.ReadLong());
		assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());

		v = 0x8000_0000_0000_0000L;
		bb.WriteLong(v);
		nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
		assertEquals(9, bb.size());
		assertEquals("80-00-00-00-00-00-00-00-00", bb.toString());
		assertEquals(v, bb.ReadLong());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		assertEquals(9, nbb.size());
		assertEquals(v, nbb.ReadLong());
		assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());

		v = -1;
		bb.WriteLong(v);
		nbb = NioByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
		assertEquals(1, bb.size());
		assertEquals("FF", bb.toString());
		assertEquals(v, bb.ReadLong());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		assertEquals(1, nbb.size());
		assertEquals(v, nbb.ReadLong());
		assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());
	}

	private static void testInt(int x) {
		ByteBuffer bb = ByteBuffer.Allocate();
		bb.WriteInt(x);
		var nbb = NioByteBuffer.Wrap(bb.Bytes, bb.WriteIndex);
		int y = bb.ReadInt();
		assertEquals(x, y);
		assertEquals(bb.ReadIndex, bb.WriteIndex);
		assertEquals(bb.WriteIndex, ByteBuffer.WriteLongSize(x));

		y = nbb.ReadInt();
		assertEquals(x, y);
		assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());
		assertEquals(nbb.getWriteIndex(), ByteBuffer.WriteLongSize(x));
	}

	private static void testLong(long x) {
		ByteBuffer bb = ByteBuffer.Allocate();
		bb.WriteLong(x);
		var nbb = NioByteBuffer.Wrap(bb.Bytes, bb.WriteIndex);
		long y = bb.ReadLong();
		assertEquals(x, y);
		assertEquals(bb.ReadIndex, bb.WriteIndex);
		assertEquals(bb.WriteIndex, ByteBuffer.WriteLongSize(x));

		y = nbb.ReadLong();
		assertEquals(x, y);
		assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());
		assertEquals(nbb.getWriteIndex(), ByteBuffer.WriteLongSize(x));
	}

	private static void testUInt(int x) {
		ByteBuffer bb = ByteBuffer.Allocate();
		ByteBuffer bb1 = ByteBuffer.Allocate();
		bb.WriteUInt(x);
		bb1.WriteULong(x & 0xffff_ffffL);
		var nbb = NioByteBuffer.Wrap(bb.Bytes, bb.WriteIndex);
		var nbb1 = NioByteBuffer.Wrap(bb1.Bytes, bb1.WriteIndex);
		assertTrue(bb.equals(bb1));
		int y = bb.ReadUInt();
		assertEquals(x, y);
		assertEquals(bb.ReadIndex, bb.WriteIndex);
		assertEquals(bb.WriteIndex, ByteBuffer.WriteUIntSize(x));
		assertEquals(bb.WriteIndex, ByteBuffer.WriteULongSize(x & 0xffff_ffffL));

		assertTrue(nbb.equals(nbb1));
		y = nbb.ReadUInt();
		assertEquals(x, y);
		assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());
		assertEquals(nbb.getWriteIndex(), ByteBuffer.WriteUIntSize(x));
		assertEquals(nbb.getWriteIndex(), ByteBuffer.WriteULongSize(x & 0xffff_ffffL));
	}

	private static void testULong(long x) {
		ByteBuffer bb = ByteBuffer.Allocate();
		bb.WriteULong(x);
		var nbb = NioByteBuffer.Wrap(bb.Bytes, bb.WriteIndex);
		long y = bb.ReadULong();
		assertEquals(x, y);
		assertEquals(bb.ReadIndex, bb.WriteIndex);
		assertEquals(bb.WriteIndex, ByteBuffer.WriteULongSize(x));

		y = nbb.ReadULong();
		assertEquals(x, y);
		assertEquals(nbb.getReadIndex(), nbb.getWriteIndex());
		assertEquals(nbb.getWriteIndex(), ByteBuffer.WriteULongSize(x));
	}

	private static void testSkipUInt(int x) {
		ByteBuffer bb = ByteBuffer.Allocate();
		bb.WriteUInt(x);
		var nbb = NioByteBuffer.Wrap(bb.Bytes, bb.WriteIndex);
		bb.ReadUInt();
		int ri = bb.ReadIndex;
		bb.ReadIndex = 0;
		bb.SkipUInt();
		assertEquals(ri, bb.ReadIndex);

		nbb.ReadUInt();
		ri = nbb.getReadIndex();
		nbb.setReadIndex(0);
		nbb.SkipUInt();
		assertEquals(ri, nbb.getReadIndex());
	}

	private static void testSkipLong(long x) {
		ByteBuffer bb = ByteBuffer.Allocate();
		bb.WriteLong(x);
		var nbb = NioByteBuffer.Wrap(bb.Bytes, bb.WriteIndex);
		bb.ReadLong();
		int ri = bb.ReadIndex;
		bb.ReadIndex = 0;
		bb.SkipLong();
		assertEquals(ri, bb.ReadIndex);

		nbb.ReadLong();
		ri = nbb.getReadIndex();
		nbb.setReadIndex(0);
		nbb.SkipLong();
		assertEquals(ri, nbb.getReadIndex());
	}

	private static void testSkipULong(long x) {
		ByteBuffer bb = ByteBuffer.Allocate();
		bb.WriteULong(x);
		var nbb = NioByteBuffer.Wrap(bb.Bytes, bb.WriteIndex);
		bb.ReadULong();
		int ri = bb.ReadIndex;
		bb.ReadIndex = 0;
		bb.SkipULong();
		assertEquals(ri, bb.ReadIndex);

		nbb.ReadULong();
		ri = nbb.getReadIndex();
		nbb.setReadIndex(0);
		nbb.SkipULong();
		assertEquals(ri, nbb.getReadIndex());
	}

	private static void testAll(long x) {
		testInt((int)x);
		testInt((int)-x);
		testUInt((int)x);
		testUInt((int)-x);
		testSkipUInt((int)x);
		testSkipUInt((int)-x);
		testLong(x);
		testLong(-x);
		testULong(x);
		testULong(-x);
		testSkipLong(x);
		testSkipLong(-x);
		testSkipULong(x);
		testSkipULong(-x);
	}

	@Test

	public void testInteger() {
		for (int i = 0; i <= 64; i++) {
			testAll(1L << i);
			testAll((1L << i) - 1);
			testAll(((1L << i) - 1) & 0x5555_5555_5555_5555L);
			testAll(((1L << i) - 1) & 0xaaaa_aaaa_aaaa_aaaaL);
		}
		var r = ThreadLocalRandom.current();
		for (int i = 0; i < 10000; i++)
			testAll(r.nextLong());
		testInt(Integer.MIN_VALUE);
		testInt(Integer.MAX_VALUE);
		testLong(Integer.MIN_VALUE);
		testLong(Integer.MAX_VALUE);
		testLong(Long.MIN_VALUE);
		testLong(Long.MAX_VALUE);
		testUInt(Integer.MIN_VALUE);
		testUInt(Integer.MAX_VALUE);
		testULong(Integer.MIN_VALUE);
		testULong(Integer.MAX_VALUE);
		testULong(Long.MIN_VALUE);
		testULong(Long.MAX_VALUE);
		testSkipLong(Integer.MIN_VALUE);
		testSkipLong(Integer.MAX_VALUE);
		testSkipLong(Long.MIN_VALUE);
		testSkipLong(Long.MAX_VALUE);
		testSkipUInt(Integer.MIN_VALUE);
		testSkipUInt(Integer.MAX_VALUE);
		testSkipULong(Integer.MIN_VALUE);
		testSkipULong(Integer.MAX_VALUE);
		testSkipULong(Long.MIN_VALUE);
		testSkipULong(Long.MAX_VALUE);
	}

	@Test

	public void testToLong() {
		var b = new byte[8];
		long vbe = 0, vle = 0;
		for (int n = 1; n <= 8; n++) {
			b[n - 1] = (byte)n;
			vbe = (vbe << 8) + n;
			vle += (long)n << ((n - 1) * 8);
			assertEquals(vbe, ByteBuffer.ToLongBE(b, 0, n));
			assertEquals(vle, ByteBuffer.ToLong(b, 0, n));
		}
	}

	@Test

	public void testBean() {
		BValue v = new BValue();
		v.setString3("abc");
		v.setBytes8(new Binary("xyz".getBytes(StandardCharsets.UTF_8)));
		Bean1 bean1 = new Bean1(123);
		bean1.getV2().put(12, 34);
		v.getList9().add(bean1);
		BSimple simple = new BSimple();
		simple.getRemoved().setInt_1(999);
		v.getMap16().put(new Key((short)11, ""), simple);

		ByteBuffer bb = ByteBuffer.Allocate();
		v.encode(bb);
		var nbb = NioByteBuffer.Wrap(bb.Bytes, bb.WriteIndex);
		BValue v2 = new BValue();
		v2.decode(bb);
		bb.ReadIndex = 0;
		ByteBuffer bb2 = ByteBuffer.Allocate();
		v2.encode(bb2);
		var nbb2 = NioByteBuffer.Wrap(bb2.Bytes, bb2.WriteIndex);

		v2 = new BValue();
		v2.decode(nbb);
		nbb.setReadIndex(0);

//		System.out.println(v);
//		System.out.println(v2);

		assertEquals(bb.size(), bb2.size());
		assertEquals(bb, bb2);

		assertEquals(nbb.size(), nbb2.size());
		assertEquals(nbb, nbb2);
	}

	@SuppressWarnings("AssertBetweenInconvertibleTypes")
	@Test
	public void testUnknown() {
		var a = new BValue();
		a.setInt_1(123);
		a.setShort5((short)456);
		var bb1 = ByteBuffer.Allocate();
		a.encode(bb1); // a:{1=123,5=456}

		var nbb1 = NioByteBuffer.Wrap(bb1.Bytes, bb1.WriteIndex);
		var b = new Bean1();
		b.decode(bb1); // b:{1=123,unknown:5=456}
		assertEquals(bb1.WriteIndex, bb1.ReadIndex);
		bb1.ReadIndex = 0;
		assertEquals(123, b.getV1());
		assertNotNull(b.unknown());
		assertTrue(b.unknown().length > 0);

		var bb2 = ByteBuffer.Allocate();
		b.encode(bb2);
		var nbb2 = NioByteBuffer.Wrap(bb2.Bytes, bb2.WriteIndex);

		b = new Bean1();
		b.decode(nbb1);
		assertEquals(nbb1.getWriteIndex(), nbb1.getReadIndex());
		nbb1.setReadIndex(0);
		assertEquals(123, b.getV1());

		a = new BValue();
		a.decode(nbb2);
		assertEquals(nbb2.getWriteIndex(), nbb2.getReadIndex());
		nbb2.setReadIndex(0);
		assertEquals(123, a.getInt_1());
		assertEquals(456, a.getShort5());
		assertEquals(nbb1, nbb2);

		var c = new BItem();
		c.decode(bb2);
		assertEquals(bb2.WriteIndex, bb2.ReadIndex);
		bb2.ReadIndex = 0;
		assertNotNull(c.unknown());
		assertTrue(c.unknown().length > 0);
		assertNotNull(c.getSubclass());
		assertEquals(0, c.getSubclass().getTypeId());
		assertTrue(c.getSubclass().getBean() instanceof EmptyBean);
		var bb3 = ByteBuffer.Allocate();
		c.encode(bb3);
		assertEquals(bb1, bb2);
		assertEquals(bb1, bb3);
		assertEquals(bb1, nbb1);
		assertEquals(bb1, nbb2);
	}

	@Test
	public void testUnknownTagSizeOverflow() {
		// 扩展delta的上限边界：0xf + 0x7FFFFFF0 = Integer.MAX_VALUE，合法接受
		// (varint编码：≥2^28的值用 F0 + 4字节大端，见testUInt的F0-10-00-00-00)
		var bb = ByteBuffer.Wrap(new byte[]{(byte)0xF1, (byte)0xF0, 0x7F, (byte)0xFF, (byte)0xFF, (byte)0xF0});
		assertEquals(Integer.MAX_VALUE, bb.ReadTagSize(bb.ReadByte()));
		// 0xf + 0x7FFFFFF1 溢出为负（0x80000000）：decode侧必须抛异常而不是静默收集负idx
		var bb2 = ByteBuffer.Wrap(new byte[]{(byte)0xF1, (byte)0xF0, 0x7F, (byte)0xFF, (byte)0xFF, (byte)0xF1});
		assertThrows(IllegalStateException.class, () -> bb2.ReadTagSize(bb2.ReadByte()));
		// 回绕到小正数的变体：0xf + 0xFFFFFFFF(int相加=14)同样是溢出流，拒绝
		var bb3 = ByteBuffer.Wrap(new byte[]{(byte)0xF1, (byte)0xF0, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF});
		assertThrows(IllegalStateException.class, () -> bb3.ReadTagSize(bb3.ReadByte()));

		// 恶意/损坏bean流（Bean1 schema={1,2}）：首tag 0xF1(扩展delta+FLOAT)，varint 0x7FFFFFF1，
		// 修复前decode静默成功并把负idx收进unknown，再encode写出伪装tag字节；修复后decode即抛异常。
		var malicious = new byte[]{(byte)0xF1, (byte)0xF0, 0x7F, (byte)0xFF, (byte)0xFF, (byte)0xF1, 0, 0, 0, 0, 0};
		assertThrows(IllegalStateException.class, () -> new Bean1().decode(ByteBuffer.Wrap(malicious.clone())));
		// NioByteBuffer走同一个default ReadTagSize，同样拒绝
		var nio1 = NioByteBuffer.Wrap(malicious.clone(), malicious.length);
		assertThrows(IllegalStateException.class, () -> new Bean1().decode(nio1));

		// 单个delta合法(=Integer.MAX_VALUE)但与已知字段id累加回绕为负的变体：
		// {1:1(0x11 0x01)} + tag 0xF1 + varint 0x7FFFFFF0 → _i_ = 1 + 0x7FFFFFFF = 0x80000000
		var wrap = new byte[]{0x11, 0x01, (byte)0xF1, (byte)0xF0, 0x7F, (byte)0xFF, (byte)0xFF, (byte)0xF0, 0, 0, 0, 0, 0};
		assertThrows(IllegalStateException.class, () -> new Bean1().decode(ByteBuffer.Wrap(wrap.clone())));
		var nio2 = NioByteBuffer.Wrap(wrap.clone(), wrap.length);
		assertThrows(IllegalStateException.class, () -> new Bean1().decode(nio2));

		// WriteTag纵深防御：负delta(降序id/负idx)不再写出伪装tag字节而是抛错
		var wb = ByteBuffer.Allocate();
		assertThrows(IllegalStateException.class, () -> wb.WriteTag(5, 3, ByteBuffer.INTEGER));
		assertThrows(IllegalStateException.class, () -> wb.WriteTag(0, Integer.MIN_VALUE, ByteBuffer.FLOAT));
		assertEquals(0, wb.WriteIndex); // 抛错时不写入任何字节
	}

	@Test
	public void testUnknownExtendedTagSize() {
		// 合法扩展delta（0xf+85=100，越过Bean1最大已知id 2）：decode收集unknown，encode原样回写，字节不变
		var bytes = new byte[]{(byte)0xF2, 0x55, 1, 2, 3, 4, 5, 6, 7, 8, 0}; // tag=扩展delta+DOUBLE, varint=85, 8字节double负载, 结束符
		var b = new Bean1();
		b.decode(ByteBuffer.Wrap(bytes.clone()));
		assertNotNull(b.unknown());
		assertTrue(b.unknown().length > 0);
		var bb = ByteBuffer.Allocate();
		b.encode(bb);
		assertArrayEquals(bytes, bb.Copy()); // 编码格式不变（协议兼容红线）
	}

	@Test
	public void testEnsureWriteOverflow() {
		// FND-Z1-4：newSize = WriteIndex + size 溢出回绕为负（或size本身为负）时，
		// 必须落入toPower2的无符号上限检查抛IllegalStateException（含ReadIndex/WriteIndex上下文），
		// 修复前静默不扩容，后续arraycopy抛无上下文的ArrayIndexOutOfBoundsException。
		var bb = ByteBuffer.Allocate();
		assertThrows(IllegalStateException.class, () -> bb.EnsureWrite(-1));
		assertThrows(IllegalStateException.class, () -> bb.EnsureWrite(Integer.MIN_VALUE)); // 溢出回绕结果的当量注入
		assertThrows(IllegalStateException.class, () -> bb.Append(new byte[0], 0, -8)); // Append(len)负len同路径
		assertThrows(IllegalStateException.class, () -> bb.ensureWriteNoCompact(-1)); // 同型方法NoCompact版

		// 单次超上限（不溢出，修复前后均抛）：锁定错误类型为ISE且信息含缓冲区状态，便于排障
		var bb2 = ByteBuffer.Allocate();
		var ex = assertThrows(IllegalStateException.class, () -> bb2.EnsureWrite(Integer.MAX_VALUE));
		assertTrue(ex.getMessage().contains("0/0"), ex.getMessage());

		// 正常路径行为不变：扩容后写入成功
		var bb3 = ByteBuffer.Allocate();
		bb3.EnsureWrite(100);
		assertTrue(bb3.Bytes.length >= 100);
		assertEquals(0, bb3.WriteIndex);
	}

	@Test
	public void testWriteStringLoneSurrogate() {
		// FND-Z1-5：未配对代理（lone surrogate）无法编码为合法UTF-8。
		// 修复前WriteString把它按普通BMP字符写出CESU-8式的0xED 0xA0-0xBF ..字节（非法UTF-8，
		// 严格解码器直接报错，宽松解码器替换），ReadString读回U+FFFD——写读一圈内容静默改变；
		// 修复后写侧替换为U+FFFD（EF BF BD），输出永远是合法UTF-8且写读对称。
		var bb = ByteBuffer.Allocate();
		assertEquals(3, ByteBuffer.utf8Size("\uD800")); // 未配对代理记3字节，与U+FFFD长度恰好一致，utf8Size无需改动
		bb.WriteString("\uD800"); // 孤立高代理
		assertArrayEquals(new byte[]{3, (byte)0xEF, (byte)0xBF, (byte)0xBD}, bb.Copy()); // 3=varint长度前缀
		assertEquals("\uFFFD", bb.ReadString());

		bb.Reset();
		bb.WriteString("\uDC00"); // 孤立低代理
		assertArrayEquals(new byte[]{3, (byte)0xEF, (byte)0xBF, (byte)0xBD}, bb.Copy());
		assertEquals("\uFFFD", bb.ReadString());

		// 混合：合法ASCII/代理对原样不变，孤立高代理与孤立低代理各替换一个U+FFFD
		// "a\uD800b\uD83D\uDE00c\uDC00"：utf8Size = 1+3+1+4+1+3 = 13
		bb.Reset();
		String s = "a\uD800b\uD83D\uDE00c\uDC00";
		assertEquals(13, ByteBuffer.utf8Size(s));
		bb.WriteString(s);
		assertArrayEquals(new byte[]{13, 'a', (byte)0xEF, (byte)0xBF, (byte)0xBD, 'b',
				(byte)0xF0, (byte)0x9F, (byte)0x98, (byte)0x80, 'c', (byte)0xEF, (byte)0xBF, (byte)0xBD}, bb.Copy());
		assertEquals("a\uFFFDb\uD83D\uDE00c\uFFFD", bb.ReadString());
	}

	@Test

	public void testCollections() {
		var r = Random.getInstance();
		for (int i = 0; i < 1000; i++) {
			var b = new BValue();
			var d = new BValue.Data();

			for (int j = 0, n = r.nextInt(3); j < n; j++) {
				b.getList30().add(j);
				d.getList30().add(j);
			}
			for (int j = 0, n = r.nextInt(3); j < n; j++) {
				b.getList31().add((long)j);
				d.getList31().add(j);
			}
			for (int j = 0, n = r.nextInt(3); j < n; j++) {
				b.getList32().add((float)j);
				d.getList32().add(j);
			}
			for (int j = 0, n = r.nextInt(3); j < n; j++) {
				b.getList33().add(new Vector2(j, j * 11));
				d.getList33().add(j);
				d.getList33().add(j * 11);
			}
			for (int j = 0, n = r.nextInt(3); j < n; j++) {
				b.getList34().add(new Vector3(j, j * 11, j * 111));
				d.getList34().add(j);
				d.getList34().add(j * 11);
				d.getList34().add(j * 111);
			}
			for (int j = 0, n = r.nextInt(3); j < n; j++) {
				b.getList35().add(new Vector4(j, j * 11, j * 111, j * 1111));
				d.getList35().add(j);
				d.getList35().add(j * 11);
				d.getList35().add(j * 111);
				d.getList35().add(j * 1111);
			}
			for (int j = 0, n = r.nextInt(3); j < n; j++) {
				b.getList36().add(new Vector2Int(j, j * 11));
				d.getList36().add(j);
				d.getList36().add(j * 11);
			}
			for (int j = 0, n = r.nextInt(3); j < n; j++) {
				b.getList37().add(new Vector3Int(j, j * 11, j * 111));
				d.getList37().add(j);
				d.getList37().add(j * 11);
				d.getList37().add(j * 111);
			}
			for (int j = 0, n = r.nextInt(3); j < n; j++) {
				b.getSet38().add(j);
				d.getSet38().add(j);
			}
			for (int j = 0, n = r.nextInt(3); j < n; j++) {
				b.getSet39().add((long)j);
				d.getSet39().add(j);
			}
			for (int j = 0, n = r.nextInt(3); j < n; j++) {
				b.getMap40().put(j, j * 111);
				d.getMap40().put(j, j * 111);
			}
			for (int j = 0, n = r.nextInt(3); j < n; j++) {
				b.getMap41().put((long)j, new BSimple(j, j * 111L, String.valueOf(j)));
				d.getMap41().put(j, new BSimple.Data(j, j * 111L, String.valueOf(j), null));
			}
			for (int j = 0, n = r.nextInt(3); j < n; j++) {
				b.getLongList().add((long)j);
				d.getLongList().add((long)j);
			}

			var bb = ByteBuffer.Allocate();
			var db = ByteBuffer.Allocate();
			b.encode(bb);
			d.encode(db);
			var nbb = NioByteBuffer.Wrap(bb.Bytes, bb.WriteIndex);
			var ndb = NioByteBuffer.Wrap(db.Bytes, db.WriteIndex);

			assertEquals(bb, db);
			b.reset();
			d.reset();
			b.decode(bb);
			d.decode(db);
			assertEquals(b.toData(), d);
			assertEquals(b, d.toBean());

			assertEquals(nbb, ndb);
			b.reset();
			d.reset();
			b.decode(nbb);
			d.decode(ndb);
			assertEquals(b.toData(), d);
			assertEquals(b, d.toBean());
		}
	}
}
