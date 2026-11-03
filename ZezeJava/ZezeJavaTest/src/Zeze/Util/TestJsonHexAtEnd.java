package Zeze.Util;

import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestJsonHexAtEnd {
	@Test
	public void overflowingHexKeepsValueAtEndOfInput() {
		for (String token : new String[]{"0x80000000", "-0x80000000", "0xffffffff",
				"0x8000000000000000", "-0x8000000000000000", "0xffffffffffffffff",
				"0x1ffffffffffffffffffff", "-0x1ffffffffffffffffffff"}) {
			assertEquals(new JsonReader().buf(token + ',').parseInt(),
					new JsonReader().buf(token).parseInt(), token + " int");
			assertEquals(new JsonReader().buf(token + ',').parseLong(),
					new JsonReader().buf(token).parseLong(), token + " long");
			assertEquals(new JsonReader().buf(token + ',').parseDouble(),
					new JsonReader().buf(token).parseDouble(), token + " double");
			var reader = new JsonReader().buf(token);
			assertEquals(new JsonReader().buf(token + ',').parseNumber(), reader.parseNumber(), token + " number");
			assertEquals(token.length(), reader.pos());
		}
		assertEquals(Integer.MAX_VALUE, new JsonReader().buf("0x80000000").parseInt());
		assertEquals(Long.MAX_VALUE, new JsonReader().buf("0x8000000000000000").parseLong());
		assertEquals(0x1.0p63, new JsonReader().buf("0x8000000000000000").parseDouble());
	}
}
