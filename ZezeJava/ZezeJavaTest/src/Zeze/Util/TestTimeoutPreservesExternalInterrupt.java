package Zeze.Util;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Fast
public class TestTimeoutPreservesExternalInterrupt {
	@Test
	public void closingBeforeTimeoutPreservesTheTaskInterrupt() {
		var bucket = new TimeoutManager.TimeoutSet(0, 1);
		try {
			try (var timeout = new TimeoutDiagnosable(bucket, () -> {})) {
				bucket.add(timeout);
				Thread.currentThread().interrupt();
			}
			Assertions.assertTrue(Thread.currentThread().isInterrupted());
		} finally {
			Thread.interrupted();
		}
	}
}
