package Zeze.Arch;

import java.net.SocketAddress;
import java.util.List;
import Zeze.Net.AsyncSocket;
import Zeze.Util.Task;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * R2-N新发现（FND7-66残留缺口）：公开API异步两段式认证形态（setAuthed前choiceProvider，
 * 见LinkdProvider.choiceProvider(link, tokenBin)的static bind路径）下，bind先于auth登记，
 * link在auth完成前关闭时onClose走未auth早退——第1轮修复只置closed拒迟到bind，不换出已登记
 * 的binds、不清provider侧linkSessionIds：条目无人再调removeLinkSession（LinkBroken按
 * "未验证通过的不通告"语义不发），泄漏到provider关闭。
 * 修复后：早退同样换出binds并执行removeLinkSession清理（LinkBroken仍只对authed发送，
 * "未验证通过的不通告"语义不变；Arch自身路径binds必空，行为不变）。
 */
@Fast
public class TestLinkdUnauthedCloseCleansBinds {

	// addSocket是protected（跨包不可见），子类暴露注册入口
	private static final class TestProviderService extends LinkdProviderService {
		TestProviderService(@NotNull String name) {
			super(name, null);
		}

		void register(@NotNull AsyncSocket so) {
			addSocket(so);
		}
	}

	private static final class StubSocket extends AsyncSocket {
		@Nullable ProviderSession userState;

		StubSocket(@Nullable ProviderSession userState) {
			super(null);
			this.userState = userState;
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
		public boolean close(@Nullable Throwable ex, boolean gracefully) {
			return true;
		}

		@Override
		public boolean Send(byte @NotNull [] bytes, int offset, int length) {
			return true;
		}

		@Override
		public @Nullable Object getUserState() {
			return userState;
		}
	}

	// 未auth会话先bind、后onClose：已登记条目必须被换出并清理provider侧linkSessionIds。
	@Test
	public void testUnauthedCloseSwapsBindsAndCleansProviderEntries() throws Exception {
		Task.tryInitThreadPool();
		var providerService = new TestProviderService("TestLusUnauthClean");
		var providerSession = new LinkdProviderSession(0);
		var providerSocket = new StubSocket(providerSession);
		providerService.register(providerSocket);

		var linkSocket = new StubSocket(null);
		var s = new LinkdUserSession(linkSocket.getSessionId());
		s.bind(providerService, linkSocket, List.of(1), providerSocket); // 未auth即bind（异步两段式形态）
		Assertions.assertEquals(providerSocket.getSessionId(), s.tryGetProvider(1), "前提：bind已登记");
		Assertions.assertTrue(containsModuleLinkSid(providerSession, 1, linkSocket.getSessionId()),
				"前提：provider侧已登记linkSessionId（泄漏载荷）");

		s.onClose(providerService); // link在auth完成前关闭

		Assertions.assertTrue(s.closed, "早退分支必须置closed（FND7-66）");
		Assertions.assertNull(s.tryGetProvider(1), "早退分支必须换出binds，不得滞留已登记条目");
		Assertions.assertFalse(containsModuleLinkSid(providerSession, 1, linkSocket.getSessionId()),
				"早退分支必须清provider侧linkSessionIds，否则条目泄漏到provider关闭");
	}

	// 兼容红线：authed会话的关闭行为不变——换出+清理+LinkBroken通告照旧。
	@Test
	public void testAuthedCloseStillCleansAndNotifies() throws Exception {
		Task.tryInitThreadPool();
		var providerService = new TestProviderService("TestLusAuthedClean");
		var providerSession = new LinkdProviderSession(0);
		var providerSocket = new StubSocket(providerSession);
		providerService.register(providerSocket);

		var linkSocket = new StubSocket(null);
		var s = new LinkdUserSession(linkSocket.getSessionId());
		s.setAuthed();
		s.bind(providerService, linkSocket, List.of(2), providerSocket);

		s.onClose(providerService);

		Assertions.assertTrue(s.closed);
		Assertions.assertNull(s.tryGetProvider(2));
		Assertions.assertFalse(containsModuleLinkSid(providerSession, 2, linkSocket.getSessionId()),
				"authed关闭清理行为不变");
	}

	// removeLinkSession在集合空时连map条目一起删：null视为"不含"。
	private static boolean containsModuleLinkSid(@NotNull LinkdProviderSession ps, int moduleId, long linkSid) {
		var sids = ps.linkSessionIds.get(moduleId);
		return sids != null && sids.contains(linkSid);
	}
}
