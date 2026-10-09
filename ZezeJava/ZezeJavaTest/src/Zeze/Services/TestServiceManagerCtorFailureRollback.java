package Zeze.Services;

import java.net.BindException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import Zeze.Config;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 构造失败必须逆序清理全部已创建资源：UDP端口被占（TCP同号端口空闲）时，
 * 构造走到Id128UdpServer.bind抛BindException——调用方拿不到实例无法stop，
 * 遗留的TCP监听与RocksDB目录锁让同进程重试永久受阻（启动状态与实际服务
 * 状态分叉）。修复：资源先在局部变量构造，成功后发布字段；失败逆序清理，
 * 清理异常作suppressed保留。
 */
@Fast
public class TestServiceManagerCtorFailureRollback {

	@Test
	public void udpConflictConstructorFailureReleasesTcpAndDb() throws Exception {
		Task.tryInitThreadPool();
		var loop = InetAddress.getLoopbackAddress();
		// autokeys参数是相对dbHome(默认".")的子目录名，必须用相对路径
		var dbDir = new java.io.File("sm_ctor_rollback_test_db");
		try (var udp = new DatagramSocket(new InetSocketAddress(loop, 0))) {
			int port = udp.getLocalPort();

			Throwable failure = null;
			try {
				new ServiceManagerServer(loop, port, new Config(), dbDir.getPath());
			} catch (Throwable e) {
				failure = e;
			}
			assertTrue(failure instanceof BindException,
					"UDP被占时构造必须以BindException失败（实际: " + failure + "）");

			// 构造失败后TCP同号端口必须可绑定（修复前遗留监听）
			try (var check = new ServerSocket()) {
				check.bind(new InetSocketAddress(loop, port));
			} catch (BindException e) {
				Assertions.fail("构造失败不得遗留TCP监听");
			}

			// DB目录锁必须已释放：同目录二次构造应再次失败在UDP bind——
			// 若第一失败遗留目录锁，第二次会在RocksDatabase构造处抛锁占用而非BindException。
			Throwable secondFailure = null;
			try (var udp2 = new DatagramSocket(new InetSocketAddress(loop, 0))) {
				try {
					new ServiceManagerServer(loop, udp2.getLocalPort(), new Config(), dbDir.getPath());
				} catch (Throwable e) {
					secondFailure = e;
				}
			}
			assertTrue(secondFailure instanceof BindException,
					"DB目录锁不得由失败对象遗留（第二次失败必须是UDP BindException而非目录锁占用，实际: "
							+ secondFailure + "）");
		} finally {
			deleteRecursively(dbDir);
		}
	}

	private static void deleteRecursively(java.io.File dir) {
		var files = dir.listFiles();
		if (files != null)
			for (var f : files)
				deleteRecursively(f);
		//noinspection ResultOfMethodCallIgnored
		dir.delete();
	}
}
