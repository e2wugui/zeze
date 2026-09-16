package UnitTest.Zeze.Util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import Zeze.Net.Binary;
import Zeze.Util.BinaryPool;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-38 回归：BinaryPool.intern(ByteBuffer,…) 语义上是只读查询，但 equals 比较把调用方
 * buffer 的 order 永久改为 LITTLE_ENDIAN 且从不恢复——调用方随后按默认 BIG_ENDIAN 的
 * getInt/getLong 静默读出错值（无任何异常信号），buffer 被共享时还是无同步的状态篡改。
 * 修复：equals 前保存 order、finally 恢复。
 */
@Fast
public class TestFnd738BinaryPoolOrder {

	@Test
	public void testInternKeepsCallerBufferOrder() {
		var pool = new BinaryPool();
		var value = 0x0102030405060708L;

		// 第一次 intern：池空走插入路径，不动调用方 buffer 的 order。
		var bb1 = ByteBuffer.allocate(8).putLong(0, value); // 默认 BIG_ENDIAN
		Binary first = pool.intern(bb1, 0, 8);
		Assertions.assertEquals(ByteOrder.BIG_ENDIAN, bb1.order(), "插入路径也不得改写order");

		// 第二次 intern 同一 buffer：命中候选触发 equals 比较——修复前在此永久改为 LITTLE_ENDIAN。
		Binary second = pool.intern(bb1, 0, 8);
		Assertions.assertSame(first, second, "等值buffer必须驻留为同一实例（intern契约）");
		Assertions.assertEquals(ByteOrder.BIG_ENDIAN, bb1.order(),
				"intern后调用方buffer的字节序必须保持不变（FND7-38）");
		Assertions.assertEquals(value, bb1.getLong(0),
				"intern后按默认序读多字节值不得静默读出错值（FND7-38）");

		// 非等值候选（同hash前缀不同内容难构造，用不同长度直接不等）也不得改写。
		var bb2 = ByteBuffer.allocate(9).putLong(0, value).put(8, (byte)9);
		Binary other = pool.intern(bb2, 0, 9);
		Assertions.assertNotSame(first, other);
		Assertions.assertEquals(ByteOrder.BIG_ENDIAN, bb2.order(), "不等路径同样不得改写order");
		Assertions.assertEquals(value, bb2.getLong(0));
	}

	@Test
	public void testInternRespectsNonDefaultOrder() {
		var pool = new BinaryPool();
		var value = 0x0102030405060708L;
		// 调用方本就用 LITTLE_ENDIAN：驻留与比较后 order 必须保持 LE，且不被错误"恢复"成 BE。
		var bb = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(0, value);
		Binary b1 = pool.intern(bb, 0, 8);
		Binary b2 = pool.intern(bb, 0, 8);
		Assertions.assertSame(b1, b2);
		Assertions.assertEquals(ByteOrder.LITTLE_ENDIAN, bb.order(), "非默认order的buffer必须原样保留");
		Assertions.assertEquals(value, bb.getLong(0));
	}
}
