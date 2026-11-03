package Zeze.Util;

import harness.Fast;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Fast
public class TestJsonUnquotedKeyComments {
	@Test
	public void commentsEndUnquotedKeysInEveryStringPath() throws ReflectiveOperationException {
		for (String key : new String[]{"name", "中文", "na\\u006de"}) {
			String decoded = key.equals("na\\u006de") ? "name" : key;
			for (String comment : new String[]{"/* note */", "// note\n"}) {
				var parsed = new JsonReader().buf("{" + key + comment + ":7,next:8}").parseMap(null);
				assertEquals(Map.of(decoded, 7, "next", 8), parsed, key + comment);
			}
		}
	}
}
