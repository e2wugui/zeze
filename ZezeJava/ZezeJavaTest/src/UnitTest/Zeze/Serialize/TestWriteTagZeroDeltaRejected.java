package UnitTest.Zeze.Serialize;

import Zeze.Serialize.ByteBuffer;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * SE1-F2 回归：WriteTag守卫原仅拦deltaId&lt;0，deltaId==0静默写出0x00-0x0f控制字节区
 * （bean结束符/父类标记），伪装tag静默损坏输出流。
 * 修复：守卫收紧为deltaId&lt;=0（与负delta守卫同类同目的）。
 */
@Fast
public class TestWriteTagZeroDeltaRejected {
	@Test
	public void testZeroAndNegativeDeltaRejected() {
		var bb = ByteBuffer.Allocate();
		Assertions.assertThrows(IllegalStateException.class, () -> bb.WriteTag(1, 1, 1),
				"deltaId==0必须拒绝（会写出控制字节区）");
		Assertions.assertThrows(IllegalStateException.class, () -> bb.WriteTag(5, 3, 1),
				"负delta仍然必须拒绝");
		Assertions.assertEquals(0, bb.WriteIndex, "拒绝路径不得写出任何字节");
		Assertions.assertEquals(1, bb.WriteTag(0, 1, 2), "正常递增id不受影响");
		Assertions.assertEquals(2, bb.WriteTag(1, 2, 1));
	}
}
