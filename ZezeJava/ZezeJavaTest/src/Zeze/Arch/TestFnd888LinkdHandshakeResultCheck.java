package Zeze.Arch;

import java.net.SocketAddress;

import Zeze.Builtin.Provider.Bind;
import Zeze.Builtin.Provider.Subscribe;
import Zeze.Net.AsyncSocket;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-88回归：OnHandshakeDone的Bind/Subscribe应答回调无条件setResult(true)，lambda
 * 参数rpc被完全忽略——超时（Zeze超时同样触发responseHandle）或错误码时完成信号仍以
 * true完成，等待该信号的应用层启动门禁误以为绑定成功，实际linkd未绑定任何模块且无
 * 任何日志；对照同文件sendDisableChoiceToLink有isTimeout/resultCode检查。
 * 修复：checkLinkdHandshakeResult检查失败→error日志（带link与方法归因）+so.close
 * （触发Connector自动重连→重握手→重发，Rpc实例一次性由重连路径以新实例重试）+
 * 失败不置位完成信号，由真实成功最终置位；成功路径照常置位。多link first-wins维持。
 */
@Fast
public class TestFnd888LinkdHandshakeResultCheck {

	private static final Zeze.Net.Service service;

	static {
		var config = new Zeze.Config();
		config.setServiceManager("disable");
		config.setNoDatabase(true);
		service = new Zeze.Net.Service("a7fnd888svc", null, config); // 裸Service不启动，仅作socket宿主
	}

	private static FakeSocket newSocket() {
		return new FakeSocket(service);
	}

	/** 最小AsyncSocket桩：记录close调用，不触碰网络。 */
	private static final class FakeSocket extends AsyncSocket {
		boolean closed;
		Throwable closeReason;

		FakeSocket(Zeze.Net.Service service) {
			super(service);
		}
		@Override
		public Type getType() {
			return Type.eClient;
		}

		@Override
		protected void doClose(@Nullable Throwable ex, boolean gracefully) {
			closed = true;
			closeReason = ex;
		}

		@Override
		public boolean Send(byte @NotNull [] bytes, int offset, int length) {
			return true;
		}

		@Override
		public @Nullable TimeThrottle getTimeThrottle() {
			return null;
		}

		@Override
		public @Nullable SocketAddress getRemoteAddress() {
			return null;
		}

		@Override
		public boolean isClosed() {
			return closed;
		}
	}

	/** 成功应答：返回true，不关socket，照常置位信号。 */
	@Test
	public void testSuccessSetsResult() {
		var so = newSocket();
		var completed = new TaskCompletionSource<Boolean>();
		Assertions.assertTrue(ProviderService.checkLinkdHandshakeResult(new Bind(), so, "a7link", "Bind"),
				"成功应答必须返回true");
		Assertions.assertFalse(so.closed, "成功不得断连");
		completed.setResult(true); // 模拟回调内置位（成功分支）
		Assertions.assertTrue(completed.isDone(), "成功路径照常置位完成信号");
	}

	/** 超时应答：返回false且断连（走重连重发），完成信号不得置位。 */
	@Test
	public void testTimeoutClosesAndNotSet() {
		var so = newSocket();
		var rpc = new Bind();
		rpc.setIsTimeout(true);
		var completed = new TaskCompletionSource<Boolean>();
		boolean ok = ProviderService.checkLinkdHandshakeResult(rpc, so, "a7link", "Bind");
		if (ok)
			completed.setResult(true); // 与生产lambda同构：仅成功才置位
		Assertions.assertFalse(ok, "超时必须返回false（原先无条件setResult(true)）");
		Assertions.assertTrue(so.closed, "失败必须断连触发重连重发");
		Assertions.assertNotNull(so.closeReason, "断连须携带原因");
		Assertions.assertFalse(completed.isDone(), "失败不得置位完成信号（应用层启动门禁不撒谎）");
	}

	/** 错误码应答：与超时同处理。 */
	@Test
	public void testErrorCodeClosesAndNotSet() {
		var so = newSocket();
		var rpc = new Subscribe();
		rpc.setResultCode(-1);
		Assertions.assertFalse(ProviderService.checkLinkdHandshakeResult(rpc, so, "a7link", "Subscribe"),
				"错误码必须返回false");
		Assertions.assertTrue(so.closed, "失败必须断连");
	}
}
