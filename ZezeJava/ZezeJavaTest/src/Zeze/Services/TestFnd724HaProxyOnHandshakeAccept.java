package Zeze.Services;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Net.ServiceConf;
import Zeze.Net.TcpSocket;
import Zeze.Services.Handshake.CHandshakeDone;
import Zeze.Services.Handshake.SHandshake0;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-24 回归（握手族覆写OnSocketAccept丢失HaProxy头安装）：HandshakeServer/HandshakeBoth
 * 覆写只调checkMaxConnections+addSocket，Service.OnSocketAccept默认实现里的HaProxyHeader
 * 安装被静默吞掉——配置了HaProxyKey时，LB的"PROXY v1"头会被当协议帧解码，所有连接被拒。
 * 修复后：提取setupHaProxyHeader为受保护方法，覆写点补调；PROXY头被正确消费并还原真实客户端地址。
 * 用EncryptType=Disable的握手流（SHandshake0→CHandshakeDone）隔离验证，不与FND7-23门禁耦合。
 * 自包含（本机随机端口），标 @Fast。
 */
@Fast
public class TestFnd724HaProxyOnHandshakeAccept {

	private static final String PROXY_LINE = "PROXY TCP4 1.2.3.4 5.6.7.8 1234 5678\r\n";

	private static int listenPort(Service service) throws Exception {
		var listener = (TcpSocket)service.newServerSocket(new InetSocketAddress("127.0.0.1", 0), null);
		var local = listener.getLocalInet();
		Assertions.assertNotNull(local);
		return local.getPort();
	}

	@Test
	public void testHandshakeServerConsumesHaProxyHeader() throws Exception {
		Task.tryInitThreadPool();
		var conf = new Config();
		var sconf = new ServiceConf();
		sconf.setHaProxyKey("TestFnd724HaProxyKey");
		conf.getServiceConfMap().put("TestFnd724Srv", sconf);

		var handshakeDone = new TaskCompletionSource<AsyncSocket>();
		var server = new HandshakeServer("TestFnd724Srv", conf) {
			@Override
			public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
				super.OnHandshakeDone(so);
				handshakeDone.setResult(so);
			}
		};
		try {
			var port = listenPort(server);

			// 模拟经LB接入的客户端：连接建立后先发PROXY v1行，再按Disable握手流回CHandshakeDone
			// （对齐HandshakeBase.processSHandshake0的Disable分支）。
			var client = new Service("TestFnd724Client", new Config()) {
				{
					AddFactoryHandle(SHandshake0.TypeId_, new Service.ProtocolFactoryHandle<>(SHandshake0::new,
							this::onSHandshake0, TransactionLevel.None, DispatchMode.Direct));
				}

				private long onSHandshake0(@NotNull SHandshake0 p) {
					CHandshakeDone.instance.Send(p.getSender());
					return 0L;
				}

				@Override
				public void OnSocketConnected(@NotNull AsyncSocket so) throws Exception {
					super.OnSocketConnected(so);
					so.Send(PROXY_LINE.getBytes(StandardCharsets.ISO_8859_1), 0, PROXY_LINE.length()); // LB视角：协议阶段前先发PROXY头
				}
			};
			try {
				client.newClientSocket("127.0.0.1", port, null, null);

				// 修复前：HaProxyHeader未安装，PROXY行被当协议帧解码→未知协议异常→连接被拒，
				// OnHandshakeDone永不触发，此处5秒超时使测试失败。
				var so = handshakeDone.get(5, TimeUnit.SECONDS);
				Assertions.assertTrue(so instanceof TcpSocket, "accept的连接必须是TcpSocket");
				var ha = ((TcpSocket)so).getHaProxyHeader();
				Assertions.assertNotNull(ha, "配置了HaProxyKey的HandshakeServer必须在accept时安装HaProxyHeader");
				var remote = ha.getRemoteAddress();
				Assertions.assertNotNull(remote, "PROXY v1头必须被解析");
				Assertions.assertEquals("1.2.3.4", remote.getAddress().getHostAddress());
				Assertions.assertEquals(1234, remote.getPort());
			} finally {
				client.stop();
			}
		} finally {
			server.stop();
		}
	}

	@Test
	public void testHandshakeBothConsumesHaProxyHeader() throws Exception {
		Task.tryInitThreadPool();
		var conf = new Config();
		var sconf = new ServiceConf();
		sconf.setHaProxyKey("TestFnd724HaProxyKey2");
		conf.getServiceConfMap().put("TestFnd724BothSrv", sconf);

		var handshakeDone = new TaskCompletionSource<AsyncSocket>();
		var server = new HandshakeBoth("TestFnd724BothSrv", conf) {
			@Override
			public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
				super.OnHandshakeDone(so);
				handshakeDone.setResult(so);
			}
		};
		try {
			var port = listenPort(server);
			var client = new Service("TestFnd724BothClient", new Config()) {
				{
					AddFactoryHandle(SHandshake0.TypeId_, new Service.ProtocolFactoryHandle<>(SHandshake0::new,
							this::onSHandshake0, TransactionLevel.None, DispatchMode.Direct));
				}

				private long onSHandshake0(@NotNull SHandshake0 p) {
					CHandshakeDone.instance.Send(p.getSender());
					return 0L;
				}

				@Override
				public void OnSocketConnected(@NotNull AsyncSocket so) throws Exception {
					super.OnSocketConnected(so);
					so.Send(PROXY_LINE.getBytes(StandardCharsets.ISO_8859_1), 0, PROXY_LINE.length());
				}
			};
			try {
				client.newClientSocket("127.0.0.1", port, null, null);
				var so = handshakeDone.get(5, TimeUnit.SECONDS);
				var ha = ((TcpSocket)so).getHaProxyHeader();
				Assertions.assertNotNull(ha, "HandshakeBoth覆写同样必须补装HaProxyHeader（FND7-24）");
				var remote = ha.getRemoteAddress();
				Assertions.assertNotNull(remote);
				Assertions.assertEquals(1234, remote.getPort());
			} finally {
				client.stop();
			}
		} finally {
			server.stop();
		}
	}
}
