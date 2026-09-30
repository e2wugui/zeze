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




}
