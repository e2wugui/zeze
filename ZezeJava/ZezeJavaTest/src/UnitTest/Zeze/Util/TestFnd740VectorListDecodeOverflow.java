package UnitTest.Zeze.Util;

import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Vector2IntList;
import Zeze.Util.Vector2List;
import Zeze.Util.Vector3IntList;
import Zeze.Util.Vector3List;
import Zeze.Util.Vector4List;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * FND7-40 回归：Vector*List.decode 先做 super.decode(bb, n*K) 再进基类负长度防线——
 * 乘法在防线之前，int 溢出让毒长度绕过校验：n=0x80000001 时 n*2==2、n=0x55555556 时
 * n*3==2、n=0x40000001 时 n*4==4，基类按极小 count 静默解码，流位置错位或静默得空表。
 * 修复：子类 decode 在乘法之前校验 n<0 || n>Integer.MAX_VALUE/K。
 */
@Fast
public class TestFnd740VectorListDecodeOverflow {

	/** 溢出后乘积为正的毒长度（各挑一个可绕过基类防线的值），流内备足分量。 */
	@Test
	public void testOverflowCountRejected() {
		// 每个用例的 buffer 预写若干分量，修复前毒长度会按极小 count 静默解码成功。
		assertDecodeRejected(() -> new Vector2IntList().decode(intBuf(4), 0x80000001)); // *2 == 2
		assertDecodeRejected(() -> new Vector2List().decode(floatBuf(4), 0x80000001)); // *2 == 2
		assertDecodeRejected(() -> new Vector3IntList().decode(intBuf(2), 0x55555556)); // *3 == 2
		assertDecodeRejected(() -> new Vector3List().decode(floatBuf(2), 0x55555556)); // *3 == 2
		assertDecodeRejected(() -> new Vector4List().decode(floatBuf(4), 0x40000001)); // *4 == 4
		// 乘积为 0（Integer.MIN_VALUE）同样静默绕过防线。
		assertDecodeRejected(() -> new Vector2IntList().decode(intBuf(0), Integer.MIN_VALUE)); // *2 == 0
		// 基类防线本就覆盖的负乘积路径不受影响（n*K 仍为负，基类抛 IAE）。
		assertDecodeRejected(() -> new Vector2IntList().decode(intBuf(0), -3));
	}

	/** 合法长度解码不受影响：正常 encode/decode 往返。 */
	@Test
	public void testLegitDecodeUnchanged() {
		var v2 = new Vector2IntList();
		v2.add(1, 2);
		v2.add(3, 4);
		var bb = ByteBuffer.Allocate();
		v2.encode(bb);
		bb.ReadIndex = 0;
		var decoded = new Vector2IntList();
		decoded.decode(bb, (int)bb.ReadUInt());
		Assertions.assertEquals(4, decoded.size(), "size()是底层分量数：2个Vector2即4个int");
		Assertions.assertEquals(1, decoded.getX(0));
		Assertions.assertEquals(4, decoded.getY(1));

		var v4 = new Vector4List();
		v4.add(1f, 2f, 3f, 4f);
		var bb4 = ByteBuffer.Allocate();
		v4.encode(bb4);
		bb4.ReadIndex = 0;
		var decoded4 = new Vector4List();
		decoded4.decode(bb4, (int)bb4.ReadUInt());
		Assertions.assertEquals(4, decoded4.size(), "size()是底层分量数：1个Vector4即4个float");
		Assertions.assertEquals(3f, decoded4.getZ(0));
	}

	private static ByteBuffer intBuf(int n) {
		var bb = ByteBuffer.Allocate();
		for (var i = 0; i < n; i++)
			bb.WriteInt(i);
		bb.ReadIndex = 0;
		return bb;
	}

	private static ByteBuffer floatBuf(int n) {
		var bb = ByteBuffer.Allocate();
		for (var i = 0; i < n; i++)
			bb.WriteFloat(i);
		bb.ReadIndex = 0;
		return bb;
	}

	private static void assertDecodeRejected(Executable decode) {
		Assertions.assertThrows(IllegalArgumentException.class, decode,
				"溢出毒长度必须在乘法之前被拒绝（FND7-40）");
	}
}
