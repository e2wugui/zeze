package Zeze.Util;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Fast
public class TestJsonUtf8Segments {

	@Test
	public void incompleteUtf8AtEndUsesTheDocumentedByteFallback() {
		for (var hex : new String[]{"c2", "e4", "e4b8", "f0", "f09f", "f09f98"}) {
			var bytes = HexFormat.of().parseHex(hex);
			assertEquals(new String(bytes, StandardCharsets.ISO_8859_1), parse(bytes, false), hex);
		}
	}

	@Test
	public void malformedScalarEncodingsUseTheDocumentedByteFallback() {
		for (var hex : new String[]{"c080", "c1bf", "e08080", "eda080", "f0808080", "f4908080", "ff808080"}) {
			var bytes = HexFormat.of().parseHex(hex);
			assertEquals(new String(bytes, StandardCharsets.ISO_8859_1), parse(bytes, true), hex);
		}
	}

	@Test
	public void validUtf8ScalarsDecodeAtBothStringBoundaries() {
		for (int codePoint : new int[]{0x80, 0x7ff, 0x800, 0xd7ff, 0xe000, 0xffff, 0x10000, 0x10ffff}) {
			var text = new String(Character.toChars(codePoint));
			var bytes = text.getBytes(StandardCharsets.UTF_8);
			assertEquals(text, parse(bytes, true));
			assertEquals(text, parse(bytes, false));
		}
	}

	private static String parse(byte[] bytes, boolean closed) {
		var input = new byte[bytes.length + (closed ? 2 : 1)];
		input[0] = '"';
		System.arraycopy(bytes, 0, input, 1, bytes.length);
		if (closed)
			input[input.length - 1] = '"';
		return new JsonReader(input).parseString();
	}
}
