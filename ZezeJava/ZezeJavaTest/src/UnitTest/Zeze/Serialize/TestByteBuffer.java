package UnitTest.Zeze.Serialize;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Vector3;
import Zeze.Serialize.Vector3Int;
import Zeze.Util.BitConverter;
import Zeze.Util.Random;
import demo.Bean1;
import demo.Module1.Key;
import demo.Module1.BSimple;
import demo.Module1.BValue;
import junit.framework.TestCase;
import org.junit.Assert;

public class TestByteBuffer extends TestCase {
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

	public void testBytes() {
		ByteBuffer bb = ByteBuffer.Allocate();
		byte[] v = ByteBuffer.Empty;
		bb.WriteBytes(v);
		assertEquals(1, bb.size());
		assertEquals("00", bb.toString());
		assertEquals(BitConverter.toString(v), BitConverter.toString(bb.ReadBytes()));
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		v = new byte[]{1, 2};
		bb.WriteBytes(v);
		assertEquals(3, bb.size());
		assertEquals("02-01-02", bb.toString());
		assertEquals(BitConverter.toString(v), BitConverter.toString(bb.ReadBytes()));
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		var str = "abc汉字123";
		bb.WriteString(str);
		assertEquals(13, bb.size());
		assertEquals("0C-61-62-63-E6-B1-89-E5-AD-97-31-32-33", bb.toString());
		assertEquals(str, bb.ReadString());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		str = new String(new char[]{0xd800 + 0x155, 0xdc00 + 0x2aa, 0xd800 + 0x2aa, 0xdc00 + 0x155}); // surrogate chars
		byte[] b0 = str.getBytes(StandardCharsets.UTF_8);
		bb.Reset();
		bb.WriteString(str);
		assertEquals(4 * 2, b0.length);
		assertEquals(4 * 2, ByteBuffer.utf8Size(str));
		assertEquals(4 * 2, bb.Bytes[0]);
		Assert.assertArrayEquals(b0, bb.ReadBytes());
		bb.ReadIndex = 0;
		assertEquals(str, bb.ReadString());
	}

	public void testBasic() {
		ByteBuffer bb = ByteBuffer.Allocate();
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		{
			boolean v = true;
			bb.WriteBool(v);
			assertEquals(1, bb.size());
			assertEquals(1, bb.Bytes[bb.ReadIndex]);
			assertEquals(v, bb.ReadBool());
			assertEquals(bb.ReadIndex, bb.WriteIndex);
		}
		{
			byte v = 1;
			bb.WriteByte(v);
			assertEquals(1, bb.size());
			assertEquals(1, bb.Bytes[bb.ReadIndex]);
			assertEquals(v, bb.ReadByte());
			assertEquals(bb.ReadIndex, bb.WriteIndex);
		}
		{
			double v = 1.1;
			bb.WriteDouble(v);
			assertEquals(8, bb.size());
			assertEquals("9A-99-99-99-99-99-F1-3F", bb.toString());
			assertEquals(v, bb.ReadDouble());
			assertEquals(bb.ReadIndex, bb.WriteIndex);
		}
		{
			float v = 1.1f;
			bb.WriteFloat(v);
			assertEquals(4, bb.size());
			assertEquals("CD-CC-8C-3F", bb.toString());
			assertEquals(v, bb.ReadFloat());
			assertEquals(bb.ReadIndex, bb.WriteIndex);
		}
		{
			int int4 = 0x12345678;
			bb.WriteInt4(int4);
			assertEquals(4, bb.size());
			assertEquals("78-56-34-12", bb.toString());
			assertEquals(int4, bb.ReadInt4());
			assertEquals(bb.ReadIndex, bb.WriteIndex);
		}
		{
			long long8 = 0x1234567801020304L;
			bb.WriteLong8(long8);
			assertEquals(8, bb.size());
			assertEquals("04-03-02-01-78-56-34-12", bb.toString());
			assertEquals(long8, bb.ReadLong8());
			assertEquals(bb.ReadIndex, bb.WriteIndex);
		}
		{
			long long8 = -12345678;
			bb.WriteLong8(long8);
			assertEquals(8, bb.size());
			assertEquals("B2-9E-43-FF-FF-FF-FF-FF", bb.toString());
			assertEquals(long8, bb.ReadLong8());
			assertEquals(bb.ReadIndex, bb.WriteIndex);
		}
		{
			long long8 = -1;
			bb.WriteLong8(long8);
			assertEquals(8, bb.size());
			assertEquals("FF-FF-FF-FF-FF-FF-FF-FF", bb.toString());
			assertEquals(long8, bb.ReadLong8());
			assertEquals(bb.ReadIndex, bb.WriteIndex);
		}
	}

	public void testUInt() {
		ByteBuffer bb = ByteBuffer.Allocate();
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		int v = 1;
		bb.WriteUInt(v);
		assertEquals(1, bb.size());
		assertEquals("01", bb.toString());
		assertEquals(v, bb.ReadUInt());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		v = 0x80;
		bb.WriteUInt(v);
		assertEquals(2, bb.size());
		assertEquals("80-80", bb.toString());
		assertEquals(v, bb.ReadUInt());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		v = 0x4000;
		bb.WriteUInt(v);
		assertEquals(3, bb.size());
		assertEquals("C0-40-00", bb.toString());
		assertEquals(v, bb.ReadUInt());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		v = 0x20_0000;
		bb.WriteUInt(v);
		assertEquals(4, bb.size());
		assertEquals("E0-20-00-00", bb.toString());
		assertEquals(v, bb.ReadUInt());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		v = 0x1000_0000;
		bb.WriteUInt(v);
		assertEquals(5, bb.size());
		assertEquals("F0-10-00-00-00", bb.toString());
		assertEquals(v, bb.ReadUInt());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		v = -1;
		bb.WriteUInt(v);
		assertEquals(5, bb.size());
		assertEquals("F0-FF-FF-FF-FF", bb.toString());
		assertEquals(v, bb.ReadUInt());
		assertEquals(bb.ReadIndex, bb.WriteIndex);
	}

	public void testLong() {
		ByteBuffer bb = ByteBuffer.Allocate();
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		long v = 1;
		bb.WriteLong(v);
		assertEquals(1, bb.size());
		assertEquals("01", bb.toString());
		assertEquals(v, bb.ReadLong());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		v = 0x80L;
		bb.WriteLong(v);
		assertEquals(2, bb.size());
		assertEquals("40-80", bb.toString());
		assertEquals(v, bb.ReadLong());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		v = 0x4000L;
		bb.WriteLong(v);
		assertEquals(3, bb.size());
		assertEquals("60-40-00", bb.toString());
		assertEquals(v, bb.ReadLong());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		v = 0x20_0000L;
		bb.WriteLong(v);
		assertEquals(4, bb.size());
		assertEquals("70-20-00-00", bb.toString());
		assertEquals(v, bb.ReadLong());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		v = 0x1000_0000L;
		bb.WriteLong(v);
		assertEquals(5, bb.size());
		assertEquals("78-10-00-00-00", bb.toString());
		assertEquals(v, bb.ReadLong());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		v = 0x8_0000_0000L;
		bb.WriteLong(v);
		assertEquals(6, bb.size());
		assertEquals("7C-08-00-00-00-00", bb.toString());
		assertEquals(v, bb.ReadLong());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		v = 0x400_0000_0000L;
		bb.WriteLong(v);
		assertEquals(7, bb.size());
		assertEquals("7E-04-00-00-00-00-00", bb.toString());
		assertEquals(v, bb.ReadLong());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		v = 0x2_0000_0000_0000L;
		bb.WriteLong(v);
		assertEquals(8, bb.size());
		assertEquals("7F-02-00-00-00-00-00-00", bb.toString());
		assertEquals(v, bb.ReadLong());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		v = 0x100_0000_0000_0000L;
		bb.WriteLong(v);
		assertEquals(9, bb.size());
		assertEquals("7F-81-00-00-00-00-00-00-00", bb.toString());
		assertEquals(v, bb.ReadLong());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		v = 0x8000_0000_0000_0000L;
		bb.WriteLong(v);
		assertEquals(9, bb.size());
		assertEquals("80-00-00-00-00-00-00-00-00", bb.toString());
		assertEquals(v, bb.ReadLong());
		assertEquals(bb.ReadIndex, bb.WriteIndex);

		v = -1;
		bb.WriteLong(v);
		assertEquals(1, bb.size());
		assertEquals("FF", bb.toString());
		assertEquals(v, bb.ReadLong());
		assertEquals(bb.ReadIndex, bb.WriteIndex);
	}

	private static void testInt(int x) {
		ByteBuffer bb = ByteBuffer.Allocate();
		bb.WriteInt(x);
		int y = bb.ReadInt();
		assertEquals(x, y);
		assertEquals(bb.ReadIndex, bb.WriteIndex);
		assertEquals(bb.WriteIndex, ByteBuffer.WriteLongSize(x));
	}

	private static void testLong(long x) {
		ByteBuffer bb = ByteBuffer.Allocate();
		bb.WriteLong(x);
		long y = bb.ReadLong();
		assertEquals(x, y);
		assertEquals(bb.ReadIndex, bb.WriteIndex);
		assertEquals(bb.WriteIndex, ByteBuffer.WriteLongSize(x));
	}

	private static void testUInt(int x) {
		ByteBuffer bb = ByteBuffer.Allocate();
		ByteBuffer bb1 = ByteBuffer.Allocate();
		bb.WriteUInt(x);
		bb1.WriteULong(x & 0xffff_ffffL);
		assertTrue(bb.equals(bb1));
		int y = bb.ReadUInt();
		assertEquals(x, y);
		assertEquals(bb.ReadIndex, bb.WriteIndex);
		assertEquals(bb.WriteIndex, ByteBuffer.WriteUIntSize(x));
		assertEquals(bb.WriteIndex, ByteBuffer.WriteULongSize(x & 0xffff_ffffL));
	}

	private static void testULong(long x) {
		ByteBuffer bb = ByteBuffer.Allocate();
		bb.WriteULong(x);
		long y = bb.ReadULong();
		assertEquals(x, y);
		assertEquals(bb.ReadIndex, bb.WriteIndex);
		assertEquals(bb.WriteIndex, ByteBuffer.WriteULongSize(x));
	}

	private static void testSkipUInt(int x) {
		ByteBuffer bb = ByteBuffer.Allocate();
		bb.WriteUInt(x);
		bb.ReadUInt();
		int ri = bb.ReadIndex;
		bb.ReadIndex = 0;
		bb.SkipUInt();
		assertEquals(ri, bb.ReadIndex);
	}

	private static void testSkipLong(long x) {
		ByteBuffer bb = ByteBuffer.Allocate();
		bb.WriteLong(x);
		bb.ReadLong();
		int ri = bb.ReadIndex;
		bb.ReadIndex = 0;
		bb.SkipLong();
		assertEquals(ri, bb.ReadIndex);
	}

	private static void testSkipULong(long x) {
		ByteBuffer bb = ByteBuffer.Allocate();
		bb.WriteULong(x);
		bb.ReadULong();
		int ri = bb.ReadIndex;
		bb.ReadIndex = 0;
		bb.SkipULong();
		assertEquals(ri, bb.ReadIndex);
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
		BValue v2 = new BValue();
		v2.decode(bb);
		bb.ReadIndex = 0;
		ByteBuffer bb2 = ByteBuffer.Allocate();
		v2.encode(bb2);

//		System.out.println(v);
//		System.out.println(v2);

		assertEquals(bb.size(), bb2.size());
		assertEquals(bb, bb2);
	}

	public void testUnknown() {
		var a = new BValue();
		a.setInt_1(123);
		a.setShort5((short)456);
		var bb1 = ByteBuffer.Allocate();
		a.encode(bb1);
		var b = new Bean1();
		b.decode(bb1);
		bb1.ReadIndex = 0;
		assertEquals(123, b.getV1());
		var bb2 = ByteBuffer.Allocate();
		b.encode(bb2);
		assertEquals(bb1, bb2);
	}

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
				b.getList33().add(new Vector3(j, j * 11, j * 111));
				d.getList33().add(j);
				d.getList33().add(j * 11);
				d.getList33().add(j * 111);
			}
			for (int j = 0, n = r.nextInt(3); j < n; j++) {
				b.getList34().add(new Vector3Int(j, j * 11, j * 111));
				d.getList34().add(j);
				d.getList34().add(j * 11);
				d.getList34().add(j * 111);
			}
			for (int j = 0, n = r.nextInt(3); j < n; j++) {
				b.getSet35().add(j);
				d.getSet35().add(j);
			}
			for (int j = 0, n = r.nextInt(3); j < n; j++) {
				b.getSet36().add((long)j);
				d.getSet36().add(j);
			}
			for (int j = 0, n = r.nextInt(3); j < n; j++) {
				b.getMap37().put(j, j * 111);
				d.getMap37().put(j, j * 111);
			}
			for (int j = 0, n = r.nextInt(3); j < n; j++) {
				b.getMap38().put((long)j, new BSimple(j, j * 111L, String.valueOf(j)));
				d.getMap38().put(j, new BSimple.Data(j, j * 111L, String.valueOf(j), null));
			}
			for (int j = 0, n = r.nextInt(3); j < n; j++) {
				b.getLongList().add((long)j);
				d.getLongList().add((long)j);
			}

			var bb = ByteBuffer.Allocate();
			var db = ByteBuffer.Allocate();
			b.encode(bb);
			d.encode(db);
			assertEquals(bb, db);
			b.reset();
			d.reset();
			b.decode(bb);
			d.decode(db);
			assertEquals(b.toData(), d);
			assertEquals(b, d.toBean());
		}
	}
}
