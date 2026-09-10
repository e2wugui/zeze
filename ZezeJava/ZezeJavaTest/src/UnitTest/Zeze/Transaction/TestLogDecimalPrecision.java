package UnitTest.Zeze.Transaction;

import java.math.BigDecimal;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Logs.LogDecimal;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND3-06 回归：LogDecimal 的 encode 写全精度字符串（value.toString()），decode 必须原样还原，
 * 保持 encode/decode 双射。修复前 decode 统一 DECIMAL128（34 位有效数字），超精度值被静默
 * 截断——follower/History 回放、重启冷加载的值与 leader 内存分叉，且 Verify 对账两侧都走
 * decode，对账也发现不了。
 */
@Fast
public class TestLogDecimalPrecision {

	private static @NotNull BigDecimal roundTrip(@NotNull BigDecimal origin) {
		var log = new LogDecimal(0);
		log.value = origin;
		var bb = ByteBuffer.Allocate();
		log.encode(bb);
		var decoded = new LogDecimal(0);
		decoded.decode(bb);
		return decoded.value;
	}

	@Test
	public void testBeyondDecimal128Precision() {
		// 45 位有效数字，超过 DECIMAL128 的 34 位
		var origin = new BigDecimal("123456789012345678901234567890123456789012345");
		Assertions.assertEquals(origin, roundTrip(origin)); // equals 含 scale，往返必须完全一致
	}

	@Test
	public void testScalePreservedWithinPrecision() {
		// ≤34 位值逐位不变（含尾零 scale），修复前后行为一致
		Assertions.assertEquals(new BigDecimal("1.100"), roundTrip(new BigDecimal("1.100")));
		Assertions.assertEquals(new BigDecimal("100"), roundTrip(new BigDecimal("100")));
	}
}
