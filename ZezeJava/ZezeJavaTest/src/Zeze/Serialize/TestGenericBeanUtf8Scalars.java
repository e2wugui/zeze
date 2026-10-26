package Zeze.Serialize;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Fast
public class TestGenericBeanUtf8Scalars {

	@Test
	public void valuesAboveUnicodeMaximumRemainBinary() {
		for (var hex : new String[]{"f4908080", "f49fbfbf", "f4bfbfbf"}) {
			var bytes = HexFormat.of().parseHex(hex);
			assertArrayEquals(bytes, assertInstanceOf(byte[].class, decode(bytes)));
		}
	}

	@Test
	public void unicodeMaximumRemainsReadableText() {
		var text = new String(Character.toChars(0x10ffff));
		assertEquals(text, decode(text.getBytes(StandardCharsets.UTF_8)));
	}

	private static Object decode(byte[] bytes) {
		var input = ByteBuffer.Allocate();
		input.WriteBytes(bytes);
		return GenericBean.decodeField(input, ByteBuffer.BYTES);
	}
}
