package Zeze.Arch;

import harness.Fast;
import java.net.SocketAddress;
import java.util.List;
import Zeze.Net.AsyncSocket;
import Zeze.Util.TimeThrottle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-66回归：LinkdUserSession.onClose对未auth会话早退不置closed——closed门只在
 * bind换出分支置位，早退后迟到的bind仍被登记（公开API的异步两段式认证形态下，
 * moduleId→linkSessionId无人再调removeLinkSession，provider侧条目泄漏）。
 * 修复后早退分支同样在写锁内置closed；Arch自身路径bind晚于auth、binds必空，置位无副作用。
 */
@Fast
public class TestFnd766LinkdUserSessionCloseFlag {

	// 未auth的onClose必须置位closed门。
	@Test
	public void testOnCloseWithoutAuthSetsClosed() {
		var s = new LinkdUserSession(123L);
		Assertions.assertFalse(s.closed);
		s.onClose(null); // 未auth早退分支（该分支不触碰linkdProviderService参数）
		Assertions.assertTrue(s.closed, "onClose早退分支必须置closed，拒绝之后的迟到bind");
	}

	// 行为闭环：未auth会话onClose后的迟到bind必须被closed门拒绝（不登记）。
	@Test
	public void testBindRejectedAfterUnauthedClose() {
		var s = new LinkdUserSession(456L);
		s.onClose(null);
		s.bind(null, null, List.of(1), new StubSocket());
		Assertions.assertNull(s.tryGetProvider(1), "closed门必须拒绝迟到bind（条目泄漏路径）");
	}

	private static final class StubSocket extends AsyncSocket {
		StubSocket() {
			super(null);
		}

		@Override
		public Type getType() {
			return Type.eServer;
		}

		@Override
		public @Nullable SocketAddress getRemoteAddress() {
			return null;
		}

		@Override
		public @Nullable TimeThrottle getTimeThrottle() {
			return null;
		}

		@Override
		public boolean isClosed() {
			return false;
		}

		@Override
		protected void doClose(@Nullable Throwable ex, boolean gracefully) {
	}

		@Override
		public boolean Send(byte @NotNull [] bytes, int offset, int length) {
			return true;
		}
	}
}
