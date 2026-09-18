package Zeze.Services;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Net.ServiceConf;
import Zeze.Net.TcpSocket;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.Handshake.CHandshake;
import Zeze.Services.Handshake.Constant;
import Zeze.Services.Handshake.SHandshake;
import Zeze.Services.Handshake.SHandshake0;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-23 回归（握手加密强制门禁）：服务端配置 EncryptType != Disable 时，不握手的明文连接
 * 直接发送应用协议，此前会被正常解码派发（verifySecurity 只挂在自愿发送的 CHandshakeDone 上），
 * 整个握手加密可被绕过。修复后输入侧逐帧门禁：未握手连接上的非握手协议必须拒绝并断连。
 * 对照组：EncryptType=Disable 的服务不受影响；正常握手完成后协议照常处理。
 * 自包含（本机随机端口、无外部依赖），标 @Fast。
 */
@Fast
public class TestFnd723HandshakeGate {

	/** 非握手应用协议（moduleId 远离握手模块 0），模拟明文直连携带的业务协议。 */
	private static final class PlaintextEcho extends Protocol<EmptyBean> {
		public static final int ModuleId_ = 0x7f23;
		public static final int ProtocolId_ = Bean.hash32(PlaintextEcho.class.getName());
		public static final long TypeId_ = ((long)ModuleId_ << 32) | (ProtocolId_ & 0xffff_ffffL);

		static {
			register(TypeId_, PlaintextEcho.class);
		}

		@Override
		public int getModuleId() {
			return ModuleId_;
		}

		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}

		PlaintextEcho() {
			Argument = EmptyBean.instance;
		}
	}

	private static int listenPort(Service service) throws Exception {
		var listener = (TcpSocket)service.newServerSocket(new InetSocketAddress("127.0.0.1", 0), null);
		var local = listener.getLocalInet();
		Assertions.assertNotNull(local);
		return local.getPort();
	}

	private static HandshakeServer newGateServer(String name, int encryptType, AtomicBoolean processed) {
		return newGateServer(name, encryptType, processed, null);
	}

	// accepted非null时在OnSocketAccept计数：单调递增的"连接已到达服务端"信号（满载假红加固用）
	private static HandshakeServer newGateServer(String name, int encryptType, AtomicBoolean processed,
												 java.util.concurrent.atomic.AtomicInteger accepted) {
		var conf = new Config();
		var sconf = new ServiceConf();
		sconf.getHandshakeOptions().setEncryptType(encryptType);
		conf.getServiceConfMap().put(name, sconf);
		return new HandshakeServer(name, conf) {
			{
				AddFactoryHandle(PlaintextEcho.TypeId_, new Service.ProtocolFactoryHandle<>(PlaintextEcho::new, p -> {
					processed.set(true);
					return 0L;
				}, TransactionLevel.None, DispatchMode.Direct));
			}

			@Override
			public void OnSocketAccept(@NotNull AsyncSocket so) throws Exception {
				super.OnSocketAccept(so);
				if (accepted != null)
					accepted.incrementAndGet();
			}
		};
	}

	private static void await(String what, int timeoutMillis, java.util.function.BooleanSupplier cond)
			throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMillis;
		while (!cond.getAsBoolean()) {
			if (System.currentTimeMillis() > deadline)
				Assertions.fail("timeout waiting: " + what);
			//noinspection BusyWait
			Thread.sleep(1);
		}
	}

	/**
	 * 用例1（主路径复现）：EncryptType=AesNoSecureIp 的 HandshakeServer，攻击者不握手、
	 * 明文直发应用协议。修复前：协议被正常处理（processed 置位、连接保持）；
	 * 修复后：连接被拒绝断开，协议绝不处理。
	 */
	@Test
	public void testPlaintextProtocolRejectedBeforeHandshake() throws Exception {
		Task.tryInitThreadPool();
		var processed = new AtomicBoolean(false);
		// 满载假红加固（R2-N）：本用例的门禁会accept后立即断连，getSocketCount()>=1是瞬态，
		// 全周期小于1ms轮询间隔时采不到（错过即恒0，等多久都假红；2026-09-16全量跑实锤10.4s超时）。
		// 改用OnSocketAccept计数的单调信号+30s预算（3x惯例），真实不变量仍是processed不得置位。
		var accepted = new java.util.concurrent.atomic.AtomicInteger();
		var server = newGateServer("TestFnd723GateSrv", Constant.eEncryptTypeAesNoSecureIp, processed, accepted);
		try {
			var port = listenPort(server);
			var attacker = new Service("TestFnd723Attacker", new Config()) {
				{
					// 注册SHandshake0/SHandshake工厂消除Unknown Protocol自断连污染（判例：
					// TestHandshakeDoneErrorClose 的教训），攻击者收到也装作无事。
					AddFactoryHandle(SHandshake0.TypeId_, new Service.ProtocolFactoryHandle<>(SHandshake0::new,
							p -> 0L, TransactionLevel.None, DispatchMode.Direct));
					AddFactoryHandle(SHandshake.TypeId_, new Service.ProtocolFactoryHandle<>(SHandshake::new,
							p -> 0L, TransactionLevel.None, DispatchMode.Direct));
				}

				@Override
				public void OnSocketConnected(@NotNull AsyncSocket so) throws Exception {
					super.OnSocketConnected(so);
					new PlaintextEcho().Send(so); // 不握手，明文直发应用协议（FND7-23 主路径）
				}
			};
			try {
				attacker.newClientSocket("127.0.0.1", port, null, null);
				await("server accepted", 30_000, () -> accepted.get() >= 1);
				await("server closed un-handshaked plaintext connection", 30_000,
						() -> server.getSocketCount() == 0);
				Assertions.assertFalse(processed.get(), "明文应用协议绝不能被处理");
			} finally {
				attacker.stop();
			}
		} finally {
			server.stop();
		}
	}

	/** 用例2（兼容红线对照）：EncryptType=Disable 的服务不受门禁影响，明文协议照常处理。 */
	@Test
	public void testDisableServiceStillProcessesPlaintext() throws Exception {
		Task.tryInitThreadPool();
		var processed = new AtomicBoolean(false);
		var server = newGateServer("TestFnd723DisableSrv", Constant.eEncryptTypeDisable, processed);
		try {
			var port = listenPort(server);
			var client = new Service("TestFnd723DisableClient", new Config()) {
				{
					AddFactoryHandle(SHandshake0.TypeId_, new Service.ProtocolFactoryHandle<>(SHandshake0::new,
							p -> 0L, TransactionLevel.None, DispatchMode.Direct));
					AddFactoryHandle(SHandshake.TypeId_, new Service.ProtocolFactoryHandle<>(SHandshake::new,
							p -> 0L, TransactionLevel.None, DispatchMode.Direct));
				}

				@Override
				public void OnSocketConnected(@NotNull AsyncSocket so) throws Exception {
					super.OnSocketConnected(so);
					new PlaintextEcho().Send(so);
				}
			};
			try {
				client.newClientSocket("127.0.0.1", port, null, null);
				await("plaintext protocol processed on Disable service", 10_000, processed::get);
			} finally {
				client.stop();
			}
		} finally {
			server.stop();
		}
	}

	/**
	 * 用例3（正向不误伤）：EncryptType=AesNoSecureIp 的服务端与 HandshakeClient 正常完成握手，
	 * 之后发送的应用协议必须照常处理，且服务端连接处于加密状态（isSecurity）。
	 */
	@Test
	public void testHandshakedConnectionStillProcessesProtocol() throws Exception {
		Task.tryInitThreadPool();
		var processed = new AtomicBoolean(false);
		var server = newGateServer("TestFnd723LegitSrv", Constant.eEncryptTypeAesNoSecureIp, processed);
		try {
			var port = listenPort(server);
			var client = new HandshakeClient("TestFnd723LegitClient", new Config()) {
				@Override
				public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
					super.OnHandshakeDone(so);
					new PlaintextEcho().Send(so); // 握手完成后才发送（走加密通道）
				}
			};
			try {
				client.newClientSocket("127.0.0.1", port, null, null);
				await("protocol processed after real handshake", 10_000, processed::get);
				Assertions.assertEquals(1, server.getSocketCount(), "合法握手连接必须保持");
				var so = server.GetSocket();
				Assertions.assertTrue(so instanceof TcpSocket tcp && tcp.isSecurity(),
						"握手完成后服务端连接必须是加密的");
			} finally {
				client.stop();
			}
		} finally {
			server.stop();
		}
	}

	/**
	 * 用例4（快照语义）：[CHandshake][明文PlaintextEcho] 拼进同一 TCP 段——第1帧合法握手被
	 * 放行并装好双向 codec，第2帧仍按缓冲区开始时的准入状态判决，明文协议不得放行。
	 */
	@Test
	public void testCoalescedHandshakeThenPlaintextStillRejected() throws Exception {
		Task.tryInitThreadPool();
		var processed = new AtomicBoolean(false);
		// 同用例1：断连后 socketCount 是瞬态，用 OnSocketAccept 计数做信号。
		var accepted = new java.util.concurrent.atomic.AtomicInteger();
		var server = newGateServer("TestFnd723CoalSrv", Constant.eEncryptTypeAesNoSecureIp, processed, accepted);
		try {
			var port = listenPort(server);
			var attacker = new Service("TestFnd723CoalAttacker", new Config()) {
				{
					// 注册SHandshake0/SHandshake工厂消除Unknown Protocol自断连污染（同用例1）。
					AddFactoryHandle(SHandshake0.TypeId_, new Service.ProtocolFactoryHandle<>(SHandshake0::new,
							p -> 0L, TransactionLevel.None, DispatchMode.Direct));
					AddFactoryHandle(SHandshake.TypeId_, new Service.ProtocolFactoryHandle<>(SHandshake::new,
							p -> 0L, TransactionLevel.None, DispatchMode.Direct));
				}

				@Override
				public void OnSocketConnected(@NotNull AsyncSocket so) throws Exception {
					super.OnSocketConnected(so);
					// 伪造合法CHandshake+明文协议拼进同一ByteBuffer单次写出，服务端单次decode同缓冲区处理。
					var ch = new CHandshake();
					ch.Argument.encryptType = Constant.eEncryptTypeAesNoSecureIp;
					ch.Argument.encryptParam = BigInteger.TEN.toByteArray();
					var bb = ByteBuffer.Allocate(256);
					appendFrame(bb, ch);
					appendFrame(bb, new PlaintextEcho());
					so.Send(bb);
				}
			};
			try {
				attacker.newClientSocket("127.0.0.1", port, null, null);
				await("server accepted", 30_000, () -> accepted.get() >= 1);
				await("server closed coalesced attack connection", 30_000, () -> server.getSocketCount() == 0);
				Assertions.assertFalse(processed.get(), "明文应用协议绝不能因同段握手帧装好codec而放行");
			} finally {
				attacker.stop();
			}
		} finally {
			server.stop();
		}
	}

	/** 完整编码一个协议帧（12字节头+参数）追加到dst。 */
	private static void appendFrame(ByteBuffer dst, Protocol<?> p) {
		var encoded = p.encode();
		dst.Append(encoded.Bytes, encoded.ReadIndex, encoded.size());
	}
}
