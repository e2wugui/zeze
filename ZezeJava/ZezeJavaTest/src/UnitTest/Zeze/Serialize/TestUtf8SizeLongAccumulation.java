package UnitTest.Zeze.Serialize;

import Zeze.Serialize.ByteBuffer;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * SE1-F1 回归：utf8Size原int累加无溢出检查，≥2^31字节的巨型字符串回绕为负，走
 * WriteString的bn&lt;=0早退被静默编码为空串（持久化数据无声丢失）。
 * 修复：long累加，≥2^31抛IllegalStateException（与EnsureWrite/ReadString的溢出拦截
 * 策略一致）。溢出分支本身不可测（构造≥2^31字节UTF-8需GB级堆），此处覆盖计数正确性
 * （1/2/3/4字节字符与代理对）及WriteString/ReadString往返一致。
 */
@Fast
public class TestUtf8SizeLongAccumulation {
	@Test
	public void testUtf8SizeCounting() {
		Assertions.assertEquals(0, ByteBuffer.utf8Size(null));
		Assertions.assertEquals(0, ByteBuffer.utf8Size(""));
		Assertions.assertEquals(5, ByteBuffer.utf8Size("hello")); // 1字节
		Assertions.assertEquals(6, ByteBuffer.utf8Size("ééé")); // U+00E9 → 2字节
		Assertions.assertEquals(6, ByteBuffer.utf8Size("中文")); // BMP → 3字节
		Assertions.assertEquals(4, ByteBuffer.utf8Size("\uD83D\uDE00")); // U+1F600 代理对 → 4字节
		Assertions.assertEquals(3, ByteBuffer.utf8Size("\uD83D")); // 孤立代理按3字节（U+FFFD替换口径）
		Assertions.assertEquals(11, ByteBuffer.utf8Size("aé中\uD83D\uDE00z")); // 1+2+3+4+1
	}

	@Test
	public void testWriteStringRoundtripMatchesUtf8Size() {
		var mixed = "aé中\uD83D\uDE00z";
		var bb = ByteBuffer.Allocate();
		bb.WriteString(mixed);
		Assertions.assertEquals(mixed, bb.ReadString());
		Assertions.assertEquals(ByteBuffer.utf8Size(mixed) + 1, bb.WriteIndex,
				"写入长度必须等于utf8Size+长度前缀（溢出早退会静默写空串）");
	}
}
