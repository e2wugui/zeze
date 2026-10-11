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

	/** 冲突口禁用OS派发（DatagramSocket(0)恰好落在49152-65535临时端口带，出站连接的源端口
	 * 也从该带派发——冲突口的TCP侧被狙时构造先死在TCP bind上，异常类型错位成
	 * TcpSocket包装的IllegalStateException而非被测的UDP BindException；2026-10-09b r38/
	 * 2026-10-11 r10两现立案）。带外选口+双栈探测：手工带（31930-31960，避开Onz GC的
	 * 31850-31911与31000）无临时端口狙击面，UDP/TCP双栈都可绑才选中（排除与其他测试
	 * 固定TCP口撞车复现同款异常类型错位）。 */
	private static int probeConflictPort(InetAddress loop) throws Exception {
		for (var port = 31930; port <= 31960; port++) {
			try (var udp = new DatagramSocket(new InetSocketAddress(loop, port));
					var tcp = new ServerSocket(port, 1, loop)) {
				return port;
			} catch (BindException e) {
				// 本口被占（UDP或TCP任一栈），试下一个
			}
		}
		throw new IllegalStateException("probe band 31930-31960 exhausted");
	}

	@Test
	public void udpConflictConstructorFailureReleasesTcpAndDb() throws Exception {
		Task.tryInitThreadPool();
		var loop = InetAddress.getLoopbackAddress();
		// autokeys参数是相对dbHome(默认".")的子目录名，必须用相对路径
		var dbDir = new java.io.File("sm_ctor_rollback_test_db");
		try (var udp = new DatagramSocket(new InetSocketAddress(loop, probeConflictPort(loop)))) {
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
