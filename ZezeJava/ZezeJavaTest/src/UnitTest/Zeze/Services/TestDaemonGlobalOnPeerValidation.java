package UnitTest.Zeze.Services;

import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Files;

import Zeze.Serialize.ByteBuffer;
import Zeze.Services.Daemon;
import Zeze.Util.LongConcurrentHashMap;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-61 回归：GlobalOn 只查 serverId 注册与索引越界、不校验报文来源对端，本地伪造
 * 合法编码的 GlobalOn 可改写守护超时配置（serverReleaseTimeout=1 即可反复销毁被监管
 * 子进程）。修复为双层校验：switch 前统一弱校验（Register 以外必须为已注册对端）+
 * GlobalOn 分支内 per-monitor 强绑定（该 serverId 的配置只能由其注册者改写，多Server
 * 共存场景同时消除跨Server改写）。本用例不经子进程，反射替换 udpSocket/monitors
 * 静态态后直驱 mainRun（subprocess为null时单命令处理即返回）。
 */
@Fast
public class TestDaemonGlobalOnPeerValidation {
	private static final int A5_SERVER_ID_A = 5959; // 本用例专属serverId，避免与其他用例冲突
	private static final int A5_SERVER_ID_B = 5960;

	private static Field staticField(Class<?> cls, String name) throws Exception {
		var f = cls.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	private static Method method(Class<?> cls, String name, Class<?>... parameterTypes) throws Exception {
		var m = cls.getDeclaredMethod(name, parameterTypes);
		m.setAccessible(true);
		return m;
	}

	/** 按sendCommand的线上格式手工编码发送，不触碰Daemon.pendings/定时器等全局态。 */
	private static void send(DatagramSocket socket, InetSocketAddress target, Daemon.Command cmd) throws Exception {
		var bb = ByteBuffer.Allocate(64);
		bb.WriteInt(cmd.command());
		cmd.encode(bb);
		socket.send(new DatagramPacket(bb.Bytes, 0, bb.WriteIndex, target));
	}

	/** 接收并解析应答为CommonResult；超时抛SocketTimeoutException（调用方按需断言）。 */
	private static Daemon.CommonResult receiveResult(DatagramSocket socket) throws Exception {
		return (Daemon.CommonResult)Daemon.receiveCommand(socket);
	}

	/** 建立注册态：合法mmap（tmpdir、zeze前缀.mmap后缀、尺寸=globalCount*8）+Monitor（不启动线程）。 */
	private static Object registerMonitor(int serverId, DatagramSocket peer) throws Exception {
		var monitorCtor = Class.forName("Zeze.Services.Daemon$Monitor")
				.getDeclaredConstructor(Daemon.Register.class);
		monitorCtor.setAccessible(true);
		var mmap = Files.createTempFile("zeze-a5-daemon-", ".mmap");
		try (var raf = new RandomAccessFile(mmap.toFile(), "rw")) {
			raf.setLength(8); // globalCount=1
		}
		var reg = new Daemon.Register(serverId, 1, mmap.toString());
		reg.peer = peer.getLocalSocketAddress();
		return monitorCtor.newInstance(reg);
	}

	@Test
	public void testGlobalOnPeerValidation() throws Exception {
		var daemonClass = Daemon.class;
		var udpSocketField = staticField(daemonClass, "udpSocket");
		var monitorsField = staticField(daemonClass, "monitors");
		var mainRun = method(daemonClass, "mainRun");
		var monitorClass = Class.forName("Zeze.Services.Daemon$Monitor");
		var getConfig = monitorClass.getDeclaredMethod("getConfig", int.class);
		getConfig.setAccessible(true);
		var stopAndJoin = monitorClass.getDeclaredMethod("stopAndJoin");
		stopAndJoin.setAccessible(true);
		@SuppressWarnings("unchecked")
		var monitors = (LongConcurrentHashMap<Object>)monitorsField.get(null);

		var savedUdp = udpSocketField.get(null);
		var udp = new DatagramSocket(0, InetAddress.getLoopbackAddress());
		udp.setSoTimeout(200);
		var target = new InetSocketAddress(InetAddress.getLoopbackAddress(), udp.getLocalPort());
		var forger = new DatagramSocket(0, InetAddress.getLoopbackAddress()); // 完全未注册的伪造者
		var legitA = new DatagramSocket(0, InetAddress.getLoopbackAddress()); // serverId A的注册对端
		var legitB = new DatagramSocket(0, InetAddress.getLoopbackAddress()); // serverId B的注册对端
		forger.setSoTimeout(2000);
		legitA.setSoTimeout(2000);
		legitB.setSoTimeout(2000);
		Object monitorA = null;
		Object monitorB = null;
		udpSocketField.set(null, udp);
		try {
			// 【第二层】未注册对端的GlobalOn在switch前被拦截：不进分支、无应答
			send(forger, target, new Daemon.GlobalOn(A5_SERVER_ID_A, 0, 1, 1));
			mainRun.invoke(null);
			Assertions.assertThrows(SocketTimeoutException.class, () -> receiveResult(forger),
				"未注册对端的命令必须整体拦截（无应答）");

			// 双Monitor注册态（Simulate多Server形态）
			monitorA = registerMonitor(A5_SERVER_ID_A, legitA);
			monitorB = registerMonitor(A5_SERVER_ID_B, legitB);
			monitors.put(A5_SERVER_ID_A, monitorA);
			monitors.put(A5_SERVER_ID_B, monitorB);

			// 【第一层】已注册但跨serverId：B的对端改写A的配置——per-monitor绑定拒绝，
			// code=1（按未注册应答隐藏区分），A的配置不生效
			send(legitB, target, new Daemon.GlobalOn(A5_SERVER_ID_A, 0, 1, 1));
			mainRun.invoke(null);
			var reply = receiveResult(legitB);
			Assertions.assertEquals(1, reply.code, "跨serverId的对端必须按未注册拒绝");
			Assertions.assertNull(getConfig.invoke(monitorA, 0), "他人改写的配置不得写入Monitor");

			// 【兼容】注册对端自身的GlobalOn照常生效：code=0，配置写入
			send(legitA, target, new Daemon.GlobalOn(A5_SERVER_ID_A, 0, 111, 222));
			mainRun.invoke(null);
			reply = receiveResult(legitA);
			Assertions.assertEquals(0, reply.code, "注册对端的合法GlobalOn不得误伤");
			var config = (Daemon.AchillesHeelConfig)getConfig.invoke(monitorA, 0);
			Assertions.assertNotNull(config);
			Assertions.assertEquals(111, config.serverDaemonTimeout);
			Assertions.assertEquals(222, config.serverReleaseTimeout);
		} finally {
			for (var m : new Object[]{monitorA, monitorB}) {
				if (m != null)
					stopAndJoin.invoke(m); // 关闭mmap句柄并删除文件
			}
			monitors.remove(A5_SERVER_ID_A);
			monitors.remove(A5_SERVER_ID_B);
			udpSocketField.set(null, savedUdp);
			udp.close();
			forger.close();
			legitA.close();
			legitB.close();
		}
	}
}
