package Zeze.Util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

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
public class TestBinaryPoolOrder {

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

	@Test
	@SuppressWarnings("unchecked")
	public void testWriteLockRecheckProbesForwardOnHashCollision() throws Exception {
		var pool = new BinaryPool();
		var mapField = BinaryPool.class.getDeclaredField("pool");
		mapField.setAccessible(true);
		var map = (LongHashMap<Binary>)Objects.requireNonNull(mapField.get(pool));
		var hashMethod = BinaryPool.class.getDeclaredMethod("hash64", long.class, byte[].class, int.class, int.class);
		hashMethod.setAccessible(true);
		var baseField = BinaryPool.class.getDeclaredField("HASH_BASE");
		baseField.setAccessible(true);
		long hashBase = baseField.getLong(null);

		byte[] payload = new byte[] {1, 2, 3, 4, 5};
		long h0 = (Long)Objects.requireNonNull(hashMethod.invoke(null, hashBase, payload, 0, payload.length));
		long h1 = (Long)Objects.requireNonNull(hashMethod.invoke(null, hashBase + 1, payload, 0, payload.length));
		var occupant = new Binary(new byte[] {9, 9, 9});
		// 模拟 rLock→wLock 窗口内另一线程向 h0 写入了哈希冲突的不同条目
		map.put(h0, occupant);

		var candidate1 = new Binary(payload.clone());
		var inserted = pool.putUnderWriteLock(h0, h0, 0, candidate1);
		Assertions.assertSame(candidate1, inserted);
		Assertions.assertSame(occupant, map.get(h0), "写锁遇到并发冲突时不得覆盖首槽既有条目");
		Assertions.assertSame(candidate1, map.get(h1), "应继续探测并写入下一个空槽");
		Assertions.assertSame(candidate1, pool.intern(payload));
		Assertions.assertSame(candidate1, pool.intern(ByteBuffer.wrap(payload), 0, payload.length));

		// 模拟窗口内首槽被冲突项占用且后续槽位已写入相同内容：写锁复查必须返回既有等值实例
		var candidate2 = new Binary(payload.clone());
		Assertions.assertSame(candidate1, pool.putUnderWriteLock(h0, h0, 0, candidate2));
		Assertions.assertSame(occupant, map.get(h0));
		Assertions.assertSame(candidate1, map.get(h1));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testProbeCoversAll32SlotsBeforeOverwrite() throws Exception {
		var pool = new BinaryPool();
		var mapField = BinaryPool.class.getDeclaredField("pool");
		mapField.setAccessible(true);
		var map = (LongHashMap<Binary>)Objects.requireNonNull(mapField.get(pool));
		var hashMethod = BinaryPool.class.getDeclaredMethod("hash64", long.class, byte[].class, int.class, int.class);
		hashMethod.setAccessible(true);
		var baseField = BinaryPool.class.getDeclaredField("HASH_BASE");
		baseField.setAccessible(true);
		long hashBase = baseField.getLong(null);

		byte[] payload = new byte[] {7, 8, 9, 10};
		long[] hashes = new long[32];
		for (int i = 0; i < 32; i++)
			hashes[i] = (Long)Objects.requireNonNull(hashMethod.invoke(null, hashBase + i, payload, 0, payload.length));

		// 占满前 31 槽（0..30），保留第 32 槽（index 31）为空
		var firstOccupant = new Binary(new byte[] {0});
		map.put(hashes[0], firstOccupant);
		for (int i = 1; i < 31; i++)
			map.put(hashes[i], new Binary(new byte[] {(byte)i}));

		var inserted = pool.intern(payload);
		Assertions.assertSame(firstOccupant, map.get(hashes[0]), "第 32 槽尚空时不得提前覆盖首槽");
		Assertions.assertSame(inserted, map.get(hashes[31]), "应完整探测到第 32 槽（HASH_BASE+31）");
		Assertions.assertSame(inserted, pool.intern(ByteBuffer.wrap(payload), 0, payload.length));
	}
}
