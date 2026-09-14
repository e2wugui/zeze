package UnitTest.Zeze.Serialize;

import java.util.HashSet;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.NioByteBuffer;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND5-46 回归：ByteBuffer.hashCode 用 calc_hashnr（FNV变体），
 * NioByteBuffer.hashCode 用 java.nio.ByteBuffer 内部 hash——两者equals
 * 按内容跨类型互等，hashCode却几乎必然不同，违反"equals相等则hashCode
 * 相等"契约；混装同一HashMap/HashSet后互相查找失败。
 * 修复：NioByteBuffer.hashCode 委托同一 calc_hashnr 算法（按剩余内容）。
 */
@Fast
public class TestByteBufferNioHashCode {

	@Test
	public void testCrossTypeHashContract() {
		var bytes = new byte[]{1, 2, 3, 4, 5, (byte)0xff};
		var bb = ByteBuffer.Wrap(bytes);
		var nio = NioByteBuffer.Wrap(bytes);
		// 前置：跨类型equals成立（既有行为）。
		Assertions.assertEquals(bb, nio, "同内容跨类型必须equals（既有行为）");
		Assertions.assertEquals(nio, bb, "equals对称（既有行为）");
		// FND5-46：equals相等则hashCode必须相等。
		Assertions.assertEquals(bb.hashCode(), nio.hashCode(),
				"同内容跨类型hashCode必须一致（FND5-46）");

		// 混装同一HashSet互相查找命中。
		var set = new HashSet<Object>();
		set.add(bb);
		Assertions.assertTrue(set.contains(nio), "HashSet跨类型查找必须命中（FND5-46）");
		set.add(nio);
		Assertions.assertEquals(1, set.size(), "equals相等的两实例同桶去重");
	}

	@Test
	public void testDifferentContentStillDiffers() {
		Assertions.assertNotEquals(
				ByteBuffer.Wrap(new byte[]{1, 2, 3}).hashCode(),
				NioByteBuffer.Wrap(new byte[]{1, 2, 4}).hashCode(),
				"不同内容哈希应不同（抽查）");
	}

	@Test
	public void testDirectBufferDelegatesSameAlgorithm() {
		// calc_hashnr(java.nio.ByteBuffer)的direct路径（无backing array）与heap产出一致。
		var bytes = new byte[]{9, 8, 7, 6, (byte)0xfe};
		var direct = NioByteBuffer.allocateDirect(bytes.length);
		direct.bb.put(bytes).flip();
		Assertions.assertFalse(direct.bb.hasArray(), "前置：direct无backing array");
		Assertions.assertEquals(ByteBuffer.Wrap(bytes).hashCode(), direct.hashCode(),
				"direct路径必须与heap同一算法产出（FND5-46复审）");
		Assertions.assertEquals(NioByteBuffer.Wrap(bytes).hashCode(), direct.hashCode());
	}
}
