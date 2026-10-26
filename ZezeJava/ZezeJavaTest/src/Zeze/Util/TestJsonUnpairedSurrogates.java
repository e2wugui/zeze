package Zeze.Util;

import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Fast
public class TestJsonUnpairedSurrogates {

	@Test
	public void everyUtf16StringHasValidUtf8JsonAndRoundTrips() throws Exception {
		var high = Character.toString((char)0xd800);
		var low = Character.toString((char)0xdc00);
		for (var text : new String[]{high, low, high + 'a', 'a' + low, low + high,
				high + high + low + low, high + low}) {
			var bytes = Json.toCompactBytes(text);
			var decoder = StandardCharsets.UTF_8.newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
			var json = decoder.decode(ByteBuffer.wrap(bytes)).toString();
			assertEquals(text, Json.parse(bytes, String.class));
			assertEquals(text, Json.parse(json, String.class));
			assertEquals(json, Json.toCompactString(text));
		}
	}
}
