package UnitTest.Zeze.Util;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.NioByteBuffer;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-15回归：Binary/ByteBuffer/NioByteBuffer的equals(Object)接受byte[]但hashCode
 * 为内容FNV、数组hashCode为身份哈希——"equals相等则hashCode相等"契约系统性破裂，
 * Binary键容器传裸数组查询会落错桶静默miss（诱导性陷阱）。修复：三处删除byte[]
 * 分支，新增显式contentEquals(byte[])（对齐String.contentEquals惯例）；
 * Binary补NioByteBuffer分支与既有NioByteBuffer.equals(Binary)对称闭合
 * （哈希三方已统一calc_hashnr）。
 */
@Fast
public class TestFnd815BinaryNoByteArrayEquals {

	private static final byte[] BYTES = "a1_fnd815_content".getBytes(StandardCharsets.UTF_8);

	@Test
	public void testByteArrayBranchRemovedWithContentEqualsExit() {
		var binary = new Binary(BYTES);
		// 修复前红：binary.equals(rawBytes)按内容为true，与身份哈希的数组违反hashCode契约
		Assertions.assertFalse(binary.equals(BYTES), "equals不再接受byte[]（身份哈希无法对齐）");
		Assertions.assertTrue(binary.contentEquals(BYTES));
		Assertions.assertFalse(binary.contentEquals("other".getBytes(StandardCharsets.UTF_8)));

		var bb = ByteBuffer.Wrap(BYTES);
		Assertions.assertFalse(bb.equals(BYTES));
		Assertions.assertTrue(bb.contentEquals(BYTES));

		var nbb = NioByteBuffer.Wrap(BYTES);
		Assertions.assertFalse(nbb.equals(BYTES));
		Assertions.assertTrue(nbb.contentEquals(BYTES));
	}

	@Test
	public void testThreeWayContentInteropSymmetric() {
		var binary = new Binary(BYTES);
		var bb = ByteBuffer.Wrap(BYTES);
		var nbb = NioByteBuffer.Wrap(BYTES);

		// 三方哈希统一（calc_hashnr），两两内容互等对称
		Assertions.assertEquals(binary.hashCode(), bb.hashCode());
		Assertions.assertEquals(binary.hashCode(), nbb.hashCode());

		Assertions.assertEquals(binary, bb);
		Assertions.assertEquals(bb, binary);
		// 修复前红：binary.equals(nbb)恒false而nbb.equals(binary)为true——不对称
		Assertions.assertEquals(binary, nbb, "Binary必须与NioByteBuffer内容互等（对称闭合）");
		Assertions.assertEquals(nbb, binary);
		Assertions.assertEquals(bb, nbb);
		Assertions.assertEquals(nbb, bb);

		// 内容哈希定位（哈希容器以Binary为键照常工作）
		var set = new HashSet<Binary>();
		set.add(binary);
		Assertions.assertTrue(set.contains(new Binary(BYTES)));
		Assertions.assertFalse(set.contains(new Binary("other".getBytes(StandardCharsets.UTF_8))));
	}
}
