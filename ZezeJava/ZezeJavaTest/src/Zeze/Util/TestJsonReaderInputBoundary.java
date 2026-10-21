package Zeze.Util;

import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestJsonReaderInputBoundary {
	@Test
	public void testSkipQuotRejectsMissingInputBeforeAdvancing() {
		var reader = new JsonReader().pos(7);
		assertThrows(NullPointerException.class, () -> reader.skipQuot('"'));
		assertEquals(7, reader.pos());
	}

	@Test
	public void testBooleanKeyRejectsMissingInputBeforeAdvancing() {
		for (var quote : new int[]{'"', '\''}) {
			var reader = new JsonReader().pos(7);
			assertThrows(NullPointerException.class, () -> JsonReader.parseBooleanKey(reader, quote));
			assertEquals(7, reader.pos());
		}
	}

	@Test
	public void testQuotedInputStillAdvancesPastClosingQuote() {
		var reader = new JsonReader().buf("a\\\"b\"tail");
		reader.skipQuot('"');
		assertEquals(5, reader.pos());

		reader.buf("\"true\":");
		assertTrue(JsonReader.parseBooleanKey(reader, '"'));
		assertEquals(6, reader.pos());
	}
}
