package harness;

import java.net.BindException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 双栈空闲口分配器（发号制+探测验证）：手工带远离OS临时口带（49152+），带内AtomicInteger
 * 发号保证同JVM并发分配互不碰撞，UDP+TCP双栈探测验证（跨JVM/外部占用时跳号重试）。
 * 由来（soak-20261126-0519 R2立案）：TestId128UdpServerValidation与
 * TestServiceManagerShutdownSuspectCleanup原用ServerSocket(0)临时口派发+双栈重绑探测，
 * 8路并行测试类的高出站连接churn下源端口同带狙击，64次重试全败（10-09b/10-11/10-13
 * 临时口带家族同机制，对齐TestServiceManagerCtorFailureRollback.probeConflictPort
 * 的带外选口先例；本类再叠加发号制，多类共享带不再互撞——发号制教义见GcOnzFastStubSupport）。
 */
public final class DualStackPortAllocator {
	private static final int BAND_START = 32200;
	private static final int BAND_END = 32399; // 手工带：全库无其他占用
	private static final AtomicInteger next = new AtomicInteger(BAND_START);

	private DualStackPortAllocator() {
	}

	public static int probe(InetAddress loop) throws Exception {
		for (int attempt = 0; attempt < 128; attempt++) {
			int candidate = BAND_START
					+ Math.floorMod(next.getAndIncrement(), BAND_END - BAND_START + 1);
			try (var udp = new DatagramSocket(new InetSocketAddress(loop, candidate));
					var tcp = new ServerSocket(candidate, 1, loop)) {
				return candidate;
			} catch (BindException ignored) {
				// 本口被占（UDP或TCP任一栈），发号跳下一口
			}
		}
		throw new IllegalStateException("failed to probe free dual-stack loopback port in band "
				+ BAND_START + "-" + BAND_END);
	}
}
