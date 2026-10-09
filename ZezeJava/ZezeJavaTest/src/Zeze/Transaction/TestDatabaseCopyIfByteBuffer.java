package Zeze.Trans;

import java.nio.ByteBuffer;

import Zeze.Transaction.Database;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Database.copyIf(java.nio.ByteBuffer) 的边界：快路径漏比position（已读游标
 * 缓冲返回带已消费前缀的整个array），慢路径把limit当数组结束下标（漏掉
 * arrayOffset，slice复制区间截短）。传入 slice/duplicate/已读游标缓冲时
 * 复制错区间。
 *
 * 修复：统一按remaining区间[position,limit)复制（数组下标补arrayOffset），
 * 快路径条件加position==0。唯一调用方（DatabaseDynamoDb，AWS SDK返回
 * position=0满缓冲）行为不变。
 */
@Fast
public class TestDatabaseCopyIfByteBuffer {

	@Test
	public void testConsumedCursorBuffer() {
		var heap = ByteBuffer.wrap(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
		heap.position(2); // 已消费前缀[0,2)，remaining=[2,10)
		Assertions.assertArrayEquals(new byte[] {2, 3, 4, 5, 6, 7, 8, 9},
				Database.copyIf(heap), "已读游标缓冲不得带出已消费前缀");
	}

	@Test
	public void testSliceBuffer() {
		var heap = ByteBuffer.wrap(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
		heap.position(2);
		var slice = heap.slice(); // 独立视角：arrayOffset=2, position=0, limit=8
		Assertions.assertArrayEquals(new byte[] {2, 3, 4, 5, 6, 7, 8, 9},
				Database.copyIf(slice), "slice的limit是子缓冲视角，复制区间须补arrayOffset");
	}

	@Test
	public void testDuplicateWithWindow() {
		var heap = ByteBuffer.wrap(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
		var dup = heap.duplicate();
		dup.position(4).limit(6); // remaining=[4,6)
		Assertions.assertArrayEquals(new byte[] {4, 5}, Database.copyIf(dup));
	}

	@Test
	public void testFastPathUnaffected() {
		var full = ByteBuffer.wrap(new byte[] {5, 6, 7}); // position=0满缓冲，共享快路径
		Assertions.assertArrayEquals(new byte[] {5, 6, 7}, Database.copyIf(full));
	}
}
