package Zeze.Services;

import java.io.InputStream;
import java.io.OutputStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestDaemonAuthorityDeadline {
	private static final class PendingExit extends Process {
		private int forceCalls;

		@Override
		public Process destroyForcibly() {
			++forceCalls;
			return this;
		}

		@Override
		public void destroy() {
			throw new AssertionError("the authority deadline requires immediate termination");
		}

		@Override
		public int exitValue() {
			throw new IllegalThreadStateException("termination has not been confirmed");
		}

		@Override
		public int waitFor() {
			throw new AssertionError("the original destroy call owns the wait");
		}

		@Override
		public OutputStream getOutputStream() {
			return OutputStream.nullOutputStream();
		}

		@Override
		public InputStream getInputStream() {
			return InputStream.nullInputStream();
		}

		@Override
		public InputStream getErrorStream() {
			return InputStream.nullInputStream();
		}
	}

	@Test
	public void deadlineForcesTerminationWhileGracefulDestroyIsInProgress() throws Exception {
		var subprocess = Daemon.class.getDeclaredField("subprocess");
		var destroying = Daemon.class.getDeclaredField("destroyingSubprocess");
		var destroy = Daemon.class.getDeclaredMethod("destroySubprocess", boolean.class);
		subprocess.setAccessible(true);
		destroying.setAccessible(true);
		destroy.setAccessible(true);
		var child = new PendingExit();
		try {
			// Snapshot of the state while the ordinary destroy call is collecting diagnostics.
			subprocess.set(null, child);
			destroying.setBoolean(null, true);
			destroy.invoke(null, true);
			assertEquals(1, child.forceCalls);
			assertSame(child, subprocess.get(null));
			assertTrue(destroying.getBoolean(null));
		} finally {
			destroying.setBoolean(null, false);
			subprocess.set(null, null);
		}
	}
}
