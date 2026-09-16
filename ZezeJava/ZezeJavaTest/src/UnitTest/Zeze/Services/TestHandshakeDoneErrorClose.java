package UnitTest.Zeze.Services;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Net.ServiceConf;
import Zeze.Net.TcpSocket;
import Zeze.Services.Handshake.CHandshakeDone;
import Zeze.Services.Handshake.Constant;
import Zeze.Services.Handshake.SHandshake0;
import Zeze.Services.Handshake.SHandshake;
import Zeze.Services.HandshakeServer;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND6-33姊妹漏网：HandshakeBase.processCHandshakeDone无catch(Throwable)+close。
 * 未握手直发CHandshakeDone时verifySecurity抛IllegalStateException，握手派发路径
 * TaskSpec.ofFunc(...).call()的OfFunc翻译吞异常（仅日志），连接滞留未握手状态——
 * 同族其他握手handler均有「握手错误不能忽略」catch+close，唯本方法漏网。
 * 断言【服务端】OnSocketClose（攻击者注册SHandshake0/SHandshake工厂消除Unknown
 * Protocol自断连污染——KeyExchange测试首版的教训）。修复前：服务端连接保持，
 * 等待超时使测试失败。
 */
@Fast
public class TestHandshakeDoneErrorClose {

	/** 计数OnSocketClose的HandshakeServer。 */
	private static final class CloseCountHandshakeServer extends HandshakeServer {
		final AtomicInteger closeCount = new AtomicInteger();

		CloseCountHandshakeServer(String name, Config config) {
			super(name, config);
		}

		@Override
		public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
			closeCount.incrementAndGet();
			super.OnSocketClose(so, e);
		}
	}

	private static int listenPort(Service service) throws Exception {
		var listener = (TcpSocket)service.newServerSocket(new InetSocketAddress("127.0.0.1", 0), null);
		var local = listener.getLocalInet();
		Assertions.assertNotNull(local);
		return local.getPort();
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

	@Test
	public void testUnhandshapedCHandshakeDoneClosesServerSide() throws Exception {
		Task.tryInitThreadPool();
		var conf = new Config();
		var sconf = new ServiceConf();
		sconf.getHandshakeOptions().setEncryptType(Constant.eEncryptTypeRsaAes);
		conf.getServiceConfMap().put("TestHsDoneErrServer", sconf);

		var server = new CloseCountHandshakeServer("TestHsDoneErrServer", conf);
		int port = listenPort(server);

		// 攻击者必须能解码SHandshake0/SHandshake（accept即发），否则自身Unknown Protocol
		// 断连污染服务端断言。
		var attacker = new Service("TestHsDoneErrAttacker", new Config()) {
			{
				AddFactoryHandle(SHandshake0.TypeId_, new Service.ProtocolFactoryHandle<>(SHandshake0::new,
						p -> 0L, TransactionLevel.None, DispatchMode.Direct));
				AddFactoryHandle(SHandshake.TypeId_, new Service.ProtocolFactoryHandle<>(SHandshake::new,
						p -> 0L, TransactionLevel.None, DispatchMode.Direct));
			}

			@Override
			public void OnSocketConnected(@NotNull AsyncSocket so) throws Exception {
				super.OnSocketConnected(so);
				CHandshakeDone.instance.Send(so); // 未握手直接宣告Done
			}
		};
		try {
			attacker.newClientSocket("127.0.0.1", port, null, null);
			// socketCount是瞬态：accept→未握手Done被拒→OnSocketClose移除可能在本await首次轮询前
			// 整段完成（低负载下更常见，20轮压测20%假红）。条件容忍"已接受并已被关闭"，
			// 关闭语义由下一个await断言。
			await("server accepted", 10_000, () -> server.getSocketCount() >= 1 || server.closeCount.get() >= 1);

			await("server OnSocketClose after unhandshaped CHandshakeDone", 10_000,
					() -> server.closeCount.get() >= 1);
		} finally {
			attacker.stop();
			server.stop();
		}
	}
}
