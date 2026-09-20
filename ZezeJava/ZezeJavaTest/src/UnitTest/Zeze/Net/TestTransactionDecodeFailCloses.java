package UnitTest.Zeze.Net;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Serialize.IByteBuffer;
import Zeze.Util.Task;
import demo.Module1.BValue;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * N2-F1回归：事务派发的错误回发回调（Protocol::trySendResultCode）在decode失败时以null调用
 * 而双重NPE，错误应答丢失、连接不关、客户端挂到Rpc超时。修复：p==null（当且仅当事务分支内
 * decodeProtocol抛出）时对齐非事务分支的"解码失败即断连"语义close连接。
 * 服务端注册decode必抛的协议（Memory库事务模式），客户端裸socket发送合法帧头——
 * 修复前连接保持打开（读挂到超时）；修复后服务端close连接，客户端读到EOF。
 */
@Fast
public class TestTransactionDecodeFailCloses {

	private static final class TestAppBase extends AppBase {
		private final Application zeze;

		TestAppBase(Application zeze) {
			this.zeze = zeze;
		}

		@Override
		public Application getZeze() {
			return zeze;
		}
	}

	// Memory库App（SM=disable，独立url避免与其他App共享存储桶）
	private static Application newMemoryApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		int serverId = NextServerId.getAndIncrement();
		conf.setServerId(serverId);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("wt4_txdecodefail_" + serverId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestTransactionDecodeFailCloses", conf);
	}

	private static final AtomicInteger NextServerId = new AtomicInteger(7300);

	public static final class BadDecodeProtocol extends Protocol<BValue> {
		public BadDecodeProtocol() {
			Argument = new BValue();
		}

		@Override
		public int getModuleId() {
			return 93;
		}

		@Override
		public int getProtocolId() {
			return 21;
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
			throw new IllegalStateException("wt4: simulated decode failure");
		}
	}

	public static final class CountingService extends Service {
		public final AtomicInteger closeCount = new AtomicInteger();

		CountingService(String name, Application app) {
			super(name, app);
		}

		@Override
		public void OnSocketClose(@NotNull AsyncSocket so, java.lang.Throwable e) {
			closeCount.incrementAndGet();
			try {
				super.OnSocketClose(so, e);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	@Test
	public void testDecodeFailInTransactionClosesSocket() throws Exception {
		Task.tryInitThreadPool();
		var app = newMemoryApp();
		app.initialize(new TestAppBase(app));
		var service = new CountingService("test.txdecodefail", app);
		try {
			Assertions.assertFalse(service.isNoProcedure(), "前提：带库形态，走事务派发分支");

			var proto = new BadDecodeProtocol();
			service.AddFactoryHandle(proto.getTypeId(), new Service.ProtocolFactoryHandle<>(BadDecodeProtocol::new));

			var listener = (TcpSocket)service.newServerSocket("127.0.0.1", 0, null);
			try {
				var local = listener.getLocalInet();
				Assertions.assertNotNull(local);
				int port = local.getPort();

				try (var sock = new Socket("127.0.0.1", port)) {
					sock.setSoTimeout(10_000);
					var bb = Zeze.Serialize.ByteBuffer.Allocate(Protocol.HEADER_SIZE);
					bb.WriteInt4(proto.getModuleId());
					bb.WriteInt4(proto.getProtocolId());
					bb.WriteInt4(0); // size=0：帧头完整
					sock.getOutputStream().write(Arrays.copyOfRange(bb.Bytes, bb.ReadIndex, bb.WriteIndex));
					sock.getOutputStream().flush();

					// 修复前：NPE吞掉错误处置，无任何响应/关闭——read阻塞到soTimeout抛异常；
					// 修复后：p==null分支close(so)，客户端读到EOF。
					InputStream is = sock.getInputStream();
					Assertions.assertEquals(-1, is.read(), "decode失败必须close连接（客户端读到EOF）");

					// 服务端善后同样到位：OnSocketClose回调、socketMap无残留
					var deadline = System.currentTimeMillis() + 10_000;
					while (service.closeCount.get() < 1 || service.getSocketCount() != 0) {
						Assertions.assertTrue(System.currentTimeMillis() < deadline, "server cleanup timeout");
						//noinspection BusyWait
						Thread.sleep(5);
					}
				}
			} finally {
				service.stop();
			}
		} finally {
			app.stop();
		}
	}
}
