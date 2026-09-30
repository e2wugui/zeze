package Zeze.Util;

import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestFuzzySearchRankingAndRemoval {
	@Test
	public void aHighUnsignedScoreRanksAheadOfALowScore() {
		var search = new StringFuzzySearch();
		search.add("aaaaa");
		search.add("a");
		var results = new String[2];
		assertEquals(2, search.search("aaaaa", results));
		assertArrayEquals(new String[]{"aaaaa", "a"}, results);
	}

	@Test
	public void aLongQueryDoesNotOverflowAccumulatedWeights() {
		var search = new StringFuzzySearch();
		search.add("aaaaa");
		search.add("a");
		var results = new String[2];
		assertEquals(2, search.search("a".repeat(65_560), results));
		assertArrayEquals(new String[]{"aaaaa", "a"}, results);
	}

	@Test
	public void deletionReclaimsEveryEmptyPostingButPreservesSharedGrams() throws Exception {
		var search = new StringFuzzySearch();
		search.add("aaaaa");
		search.add("aaaaab");
		assertTrue(search.remove("aaaaa"));
		var results = new String[2];
		assertEquals(1, search.search("aaaaab", results));
		assertEquals("aaaaab", results[0]);
		assertTrue(search.remove("aaaaab"));
		for (var i = 0; i < 100; i++) {
			var text = "unique-" + i;
			assertTrue(search.add(text));
			assertTrue(search.remove(text));
		}
		assertEquals(0, search.size());
		// size()==0不能证明倒排数组已回收：检查四个索引不再强持有历史空posting。
		for (var name : new String[]{"index1", "index2", "index3", "index4"}) {
			var field = StringFuzzySearch.class.getDeclaredField(name);
			field.setAccessible(true);
			var index = field.get(search);
			assertEquals(0, ((Number)index.getClass().getMethod("size").invoke(index)).intValue(), name);
		}
		assertTrue(search.add("aaaaa"));
		assertEquals(1, search.search("aaaaa", results));
		assertEquals("aaaaa", results[0]);
	}
}
