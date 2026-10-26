package Zeze.Services;

import Zeze.Services.ServiceManager.Tid128Cache;
import Zeze.Util.Id128;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestTid128GrantOwnership {
	@Test
	public void constructorDoesNotRetainMutableGrant() {
		var start = new Id128(3, 100);
		var cache = new Tid128Cache("grant", null, start, 16);
		start.assign(0, 0);
		assertEquals(new Id128(3, 100), cache.next());
		assertEquals(new Id128(3, 100), cache.getStart());
	}

	@Test
	public void observationsCannotRewindAllocation() {
		var cache = new Tid128Cache("grant", null, new Id128(3, 100), 16);
		assertEquals(new Id128(3, 100), cache.next());
		cache.get().assign(0, 0);
		cache.getStart().assign(0, 0);
		assertEquals(new Id128(3, 101), cache.next());
		assertEquals(new Id128(3, 100), cache.getStart());
	}

	@Test
	public void issuedFirstIdDoesNotChangeGrantMetadata() {
		var cache = new Tid128Cache("grant", null, new Id128(3, 100), 16);
		cache.next().assign(0, 0);
		assertEquals(new Id128(3, 100), cache.getStart());
		assertEquals(new Id128(3, 101), cache.next());
	}
}
