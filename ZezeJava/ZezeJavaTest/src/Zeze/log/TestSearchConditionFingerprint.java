package Zeze.log;

import Zeze.log.handle.entity.SearchLogParam;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Fast
public class TestSearchConditionFingerprint {
	@Test
	public void differentConditionsMustNotReuseBinding() throws Exception {
		var first = condition("foo]|bar", "baz");
		var second = condition("foo", "bar]|baz");
		assertNull(first.validateError(false));
		assertNull(second.validateError(false));
		for (var browse : new boolean[]{false, true}) {
			var binding = LogSessionBinding.server("server", "log",
					first.conditionFingerprint(browse), new Object());
			assertFalse(binding.matches(false, "server", "log", second.conditionFingerprint(browse)),
					"changing a condition must invalidate the old cursor even when it contains delimiters");
		}
	}

	@Test
	public void normalizedConditionsKeepTheSameFingerprint() throws Exception {
		var first = condition(" error , , warn ", "[|\\\"]");
		var second = condition("error,warn", "[|\\\"]");
		assertEquals(first.conditionFingerprint(false), second.conditionFingerprint(false));
		assertEquals(first.conditionFingerprint(true), second.conditionFingerprint(true));
		assertNotEquals(first.conditionFingerprint(false), first.conditionFingerprint(true));
		second.setOffsetFactor(0.5f);
		assertEquals(first.conditionFingerprint(false), second.conditionFingerprint(false));
		assertNotEquals(first.conditionFingerprint(true), second.conditionFingerprint(true));
	}

	private static SearchLogParam condition(String words, String pattern) {
		var result = new SearchLogParam();
		result.setWords(words);
		result.setPattern(pattern);
		result.setLimit(10);
		return result;
	}
}
