package Zeze.Services;

import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestRaftGcmReconnectWindow {
	@Test
	void reconnectWindowUsesElapsedNanosAndTheCurrentTerm() {
		long readyNanos = Long.MAX_VALUE - 500_000;
		var leadership = new GlobalCacheManagerWithRaft.Leadership(7, readyNanos);
		assertFalse(leadership.reconnectWindowElapsed(7, readyNanos + 1_000_000, 1));
		assertFalse(leadership.reconnectWindowElapsed(8, readyNanos + 1_000_001, 1));
		assertTrue(leadership.reconnectWindowElapsed(7, readyNanos + 1_000_001, 1));
		var reelected = new GlobalCacheManagerWithRaft.Leadership(8, readyNanos + 2_000_000);
		assertFalse(reelected.reconnectWindowElapsed(8, readyNanos + 2_000_001, 1));
	}
}
