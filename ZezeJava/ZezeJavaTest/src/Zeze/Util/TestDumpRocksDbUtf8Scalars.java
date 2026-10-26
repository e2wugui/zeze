package Zeze.Util;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import Zeze.Serialize.ByteBuffer;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestDumpRocksDbUtf8Scalars {

	@Test
	public void malformedScalarEncodingsRemainEscapedBinary() throws Exception {
		for (var hex : new String[]{"e08080", "e09fbf", "eda080", "edbfbf",
				"f0808080", "f08fbfbf", "f4908080", "f4bfbfbf"}) {
			var dumped = dumpBytes(HexFormat.of().parseHex(hex));
			assertTrue(dumped.startsWith("'") && dumped.endsWith("'"), hex + ": " + dumped);
			assertTrue(dumped.chars().allMatch(c -> c < 128), hex + " must not emit invalid UTF-8");
		}
	}

	@Test
	public void validScalarBoundariesRemainReadableText() throws Exception {
		for (int codePoint : new int[]{0x800, 0xd7ff, 0xe000, 0xffff, 0x10000, 0x10ffff}) {
			var text = new String(Character.toChars(codePoint));
			assertEquals('"' + text + '"', dumpBytes(text.getBytes(StandardCharsets.UTF_8)));
		}
	}

	private static String dumpBytes(byte[] bytes) throws Exception {
		var input = ByteBuffer.Allocate();
		input.WriteBytes(bytes);
		var output = new ByteArrayOutputStream();
		var method = DumpRocksDb.class.getDeclaredMethod("dumpVar", OutputStream.class, ByteBuffer.class, int.class);
		method.setAccessible(true);
		method.invoke(null, output, input, ByteBuffer.BYTES);
		return output.toString(StandardCharsets.UTF_8);
	}
}
