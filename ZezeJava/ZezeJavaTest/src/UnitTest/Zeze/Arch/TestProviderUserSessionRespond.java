package UnitTest.Zeze.Arch;

import Zeze.Arch.ProviderUserSession;
import Zeze.Builtin.Online.Login;
import Zeze.Builtin.Online.SReliableNotify;
import Zeze.Builtin.Provider.Dispatch;
import Zeze.Net.Binary;
import Zeze.Net.Rpc;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * ProviderUserSession.respond 家族的时序语义单元测试（不组网，Probe 拦截真实发送）。
 * 词汇表与 OnlineSpec 同构：respond=事务感知（事务内延迟到提交、回滚不发），respondNow=立即，
 * respondWhileRollback=回滚时，respondFireAndForget=事务感知+绕过 Online 记账。
 */
public class TestProviderUserSessionRespond {
	@BeforeEach
	public final void testInit() throws Exception {
		demo.App.getInstance().Start();
	}

	@AfterEach
	public final void testCleanup() throws Exception {
		//demo.App.getInstance().Stop();
	}

	private static final class ProbeSession extends ProviderUserSession {
		int accounted;
		int fireAndForget;

		ProbeSession(@NotNull Dispatch dispatch) {
			super(dispatch);
		}

		@Override
		protected boolean sendAccounted(long typeId, @NotNull Binary fullEncodedProtocol) {
			accounted++;
			return true;
		}

		@Override
		protected void sendFireAndForgetReal(@NotNull Rpc<?, ?> rpc) {
			fireAndForget++;
		}
	}

	@Test
	public void testRespondTiming() {
		Transaction.destroy(); // 保证起点无事务（同线程可能残留前序测试的事务）

		// 无事务：respond 立即发送
		var s1 = new ProbeSession(new Dispatch());
		s1.respond(new Login());
		Assertions.assertEquals(1, s1.accounted, "respond outside transaction must send immediately");

		// 事务内：respond 延迟到提交后发送
		var s2 = new ProbeSession(new Dispatch());
		demo.App.getInstance().Zeze.newProcedure(() -> {
			s2.respond(new Login());
			Assertions.assertEquals(0, s2.accounted, "respond inside transaction must not send before commit");
			return Procedure.Success;
		}, "respond-defer").call();
		Assertions.assertEquals(1, s2.accounted, "respond must send after commit");

		// 事务内：respondNow 立即发送（不等提交）
		var s3 = new ProbeSession(new Dispatch());
		demo.App.getInstance().Zeze.newProcedure(() -> {
			s3.respondNow(new Login());
			Assertions.assertEquals(1, s3.accounted, "respondNow must send immediately even inside transaction");
			return Procedure.Success;
		}, "respondNow").call();
		Assertions.assertEquals(1, s3.accounted);

		// 事务回滚：respond 注册的发送不发出
		var s4 = new ProbeSession(new Dispatch());
		demo.App.getInstance().Zeze.newProcedure(() -> {
			s4.respond(new Login());
			return Procedure.Exception;
		}, "respond-rollback-drop").call();
		Assertions.assertEquals(0, s4.accounted, "respond must not send when transaction rolls back");
	}

	@Test
	public void testRespondWhileRollback() {
		var s = new ProbeSession(new Dispatch());
		demo.App.getInstance().Zeze.newProcedure(() -> {
			s.respondWhileRollback(new Login());
			Assertions.assertEquals(0, s.accounted, "respondWhileRollback must not send before rollback");
			return Procedure.Exception;
		}, "respondWhileRollback").call();
		Assertions.assertEquals(1, s.accounted, "respondWhileRollback must send after rollback");
	}

	@Test
	public void testRespondFireAndForget() {
		Transaction.destroy();

		// 无事务：立即经 fire-and-forget 路径发送，且不经记账
		var s1 = new ProbeSession(new Dispatch());
		s1.respondFireAndForget(new Login());
		Assertions.assertEquals(1, s1.fireAndForget);
		Assertions.assertEquals(0, s1.accounted, "fire-and-forget must bypass Online accounting");

		// 事务内：延迟到提交后经 fire-and-forget 路径发送，且不经记账
		var s2 = new ProbeSession(new Dispatch());
		demo.App.getInstance().Zeze.newProcedure(() -> {
			s2.respondFireAndForget(new Login());
			Assertions.assertEquals(0, s2.fireAndForget, "fire-and-forget inside transaction must defer to commit");
			return Procedure.Success;
		}, "respondFireAndForget-defer").call();
		Assertions.assertEquals(1, s2.fireAndForget);
		Assertions.assertEquals(0, s2.accounted, "fire-and-forget must bypass Online accounting");
	}

	@Test
	public void testTryRespondErrorNow() {
		var s = new ProbeSession(new Dispatch());

		// 非 Rpc：静默跳过
		s.tryRespondErrorNow(new SReliableNotify(), 12345);
		Assertions.assertEquals(0, s.accounted, "non-Rpc must be silently skipped");

		// 已回复过的 Rpc（非 request 状态）：静默跳过
		var responded = new Login();
		responded.setRequest(false);
		s.tryRespondErrorNow(responded, 12345);
		Assertions.assertEquals(0, s.accounted, "already-responded rpc must be silently skipped");

		// 未回复的 Rpc 请求：立即带错误码回
		var pending = new Login();
		s.tryRespondErrorNow(pending, 12345);
		Assertions.assertEquals(1, s.accounted, "pending rpc request must be responded immediately");
		Assertions.assertEquals(12345, pending.getResultCode());
	}
}
