package UnitTest.Zeze.Net;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND5-18 回归：对监听socket（eServerSocket）调closeGracefully()——优雅路径
 * 的addInterestOps(OP_WRITE)对ServerSocketChannel（validOps仅OP_ACCEPT）
 * 抛IllegalArgumentException，且发生在120s兜底realClose注册之前：channel
 * 未关、closed已置1，后续任何close()因CAS失败直接返回false，监听端口永久
 * 泄漏不可再关。修复：监听socket不走优雅路径（无输出缓冲，优雅语义本不
 * 适用），直接realClose。仓内Acceptor.Stop()用非优雅close()，本缺陷由
 * 公开API误用可达。
 */
@Fast
public class TestServerSocketGracefulClose {

	static {
		Task.tryInitThreadPool();
	}

	@Test
	public void testGracefulCloseOnListenSocket() throws Exception {
		var server = new Service("TestServerSocketGracefulClose");
		try {
			var listen = (TcpSocket)server.newServerSocket("127.0.0.1", 0, null);
			Assertions.assertEquals(AsyncSocket.Type.eServerSocket, listen.getType());

			// FND5-18核心：优雅关闭监听socket不得抛（修复前interestOpsOr抛IllegalArgumentException）。
			Assertions.assertDoesNotThrow(() -> Assertions.assertTrue(listen.close(null, true)),
					"对监听socket调closeGracefully不得抛IllegalArgumentException（FND5-18）");

			// channel真实关闭（端口随channel.close同步释放；不用重绑探测——并行套件下
			// Windows临时端口竞争会让重绑天然flaky）。
			Assertions.assertTrue(listen.isClosed());
			var keyField = TcpSocket.class.getDeclaredField("selectionKey");
			keyField.setAccessible(true);
			var channel = ((java.nio.channels.SelectionKey)keyField.get(listen)).channel();
			Assertions.assertFalse(channel.isOpen(), "监听channel必须真实关闭（FND5-18）");
		} finally {
			server.stop();
		}
	}
}
