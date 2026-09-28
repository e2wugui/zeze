package Zeze.Services.ZokerImpl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Builtin.Zoker.StartService;
import Zeze.Builtin.Zoker.StopService;
import Zeze.IModule;
import Zeze.Services.Zoker;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND22 GE-C02：stopService 首行摘账、此后最长 10s优雅+10s强杀 的停机窗口内，并发 startService
 * 按 run.pid 领养"正在被终止"的进程并回执 Running——回执即谎言且终局服务死（GE-D01(FND21)
 * 领养落地引入的行为退化；领养判据只看 pid 存活+指纹相符，无法区分现役与正被杀）。
 * 修复：同服务 start/stop 全程持 opsLocks（services/&lt;svc&gt; 折叠键，对齐 commitLocks 形态）。
 * <ul>
 * <li><b>确定性红</b>（核心）：注入"慢死"假句柄（destroyForcibly 延迟真杀）复现摘账-停机窗口，
 * 窗口内并发 start 的终局断言"回执 Running ⇒ 记账条目真活着"。修复前：领养条目随进程死亡
 * 被 watchExit 摘除/或句柄已死 → 红；修复后：start 阻塞到 stop 完成（无身份/死残留已清），
 * 重新拉起真活进程 → 绿。run.pid 用真实进程身份摆盘（startInstant 指纹真实可核）。</li>
 * <li><b>持锁结构判别</b>（全平台）：外部持有锁对象时 start/stop 都必须阻塞——"全程持锁"
 * 的直接验证；锁键大小写折叠（TestFnd21E02 同论证）。</li>
 * </ul>
 */
@Fast
public class TestE02StartStopMutex {
	private static final long NO_PROPS = IModule.errorCode(Zoker.ModuleId, Zoker.eNoServiceProperties);
	private static final boolean WINDOWS =
			System.getProperty("os.name", "").toLowerCase().contains("win");

	/** 字段注入（每方法新实例=每方法独立目录）：@AfterEach 先行兜底删除见 {@link TempDirBestEffort}。 */
	@TempDir
	Path tempDir;

	@AfterEach
	void cleanupTempDir() {
		TempDirBestEffort.delete(tempDir);
	}

	private static File servicesDir(Path tempDir) throws IOException {
		var f = tempDir.resolve("services").toFile();
		Files.createDirectories(f.toPath());
		return f;
	}

	private static void layoutVersion(File servicesDir, String props) throws IOException {
		var svc = servicesDir.toPath().resolve("svc");
		var v1 = Files.createDirectories(svc.resolve("v1"));
		Files.writeString(svc.resolve(DistributeManager.CURRENT_NAME), "v1");
		Files.writeString(v1.resolve(ServiceManager.SERVICE_PROPERTIES_NAME), props);
	}

	private static StartService startReq() {
		var r = new StartService();
		r.Argument.setServiceName("svc");
		return r;
	}

	private static StopService stopReq(boolean force) {
		var r = new StopService();
		r.Argument.setServiceName("svc");
		r.Argument.setForce(force);
		return r;
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<String, Object> opsLocksOf(ServiceManager sm) throws Exception {
		Field field = ServiceManager.class.getDeclaredField("opsLocks");
		field.setAccessible(true);
		return (ConcurrentHashMap<String, Object>)field.get(sm);
	}

	/**
	 * 慢死假句柄：身份/存活/退出码全委托真进程，destroy/destroyForcibly 延迟 delayMillis 才
	 * 真杀——确定性构造"已摘账、进程未死"的停机窗口（GE-C02 竞态的前提时序）。
	 */
	private static final class SlowDieProcess extends Process {
		private final Process real;
		private final long delayMillis;

		SlowDieProcess(Process real, long delayMillis) {
			this.real = real;
			this.delayMillis = delayMillis;
		}

		private void delayedKill() {
			var killer = new Thread(() -> {
				try {
					Thread.sleep(delayMillis);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				real.destroyForcibly();
			}, "slow-die-killer");
			killer.setDaemon(true);
			killer.start();
		}

		@Override
		public long pid() {
			return real.pid();
		}

		@Override
		public boolean isAlive() {
			return real.isAlive();
		}

		@Override
		public int exitValue() {
			return real.exitValue();
		}

		@Override
		public void destroy() {
			delayedKill();
		}

		@Override
		public Process destroyForcibly() {
			delayedKill();
			return this;
		}

		@Override
		public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
			return real.waitFor(timeout, unit);
		}

		@Override
		public int waitFor() throws InterruptedException {
			return real.waitFor();
		}

		@Override
		public OutputStream getOutputStream() {
			throw new UnsupportedOperationException();
		}

		@Override
		public InputStream getInputStream() {
			throw new UnsupportedOperationException();
		}

		@Override
		public InputStream getErrorStream() {
			throw new UnsupportedOperationException();
		}
	}

	/** 核心红点（Windows 真进程）：停机窗口（摘账后、进程死前）内并发 start 的回执不得是
	 * 谎言——终局记账条目必须真活着。修复前：start 领养正被杀的进程回执 Running(adopted)，
	 * 进程随后死亡、条目被收殓；修复后：start 串行化到 stop 之后，重新拉起。 */
	@Test
	public void testStartDuringKillWindowRelaunchesInsteadOfAdoptingDying() throws Exception {
		Assumptions.assumeTrue(WINDOWS, "最小真进程形态为Windows命令（ping）");
		var servicesDir = servicesDir(tempDir);
		layoutVersion(servicesDir, "command=ping\nargs=-n 60 127.0.0.1\n");
		var sm = new ServiceManager(servicesDir);

		// 摆"已拉起"形态：真进程 + 记账注入慢死假句柄 + 盘上真实身份（指纹可核）
		var real = new ProcessBuilder("ping", "-n", "60", "127.0.0.1").start();
		try {
			var container = servicesDir.toPath().resolve("svc").toFile();
			var startInstant = ProcessHandle.of(real.pid()).orElseThrow()
					.info().startInstant().map(Object::toString).orElse("");
			assertFalse(startInstant.isEmpty(), "探针前提：真进程 startInstant 可得");
			ServiceManager.writeRunPid(container,
					new ServiceManager.RunPidRecord(real.pid(), startInstant, "slow-die"));
			sm.putProcessForTest("svc", new SlowDieProcess(real, 1_500));

			var stopReceipt = new AtomicReference<StopService>();
			var startReceipt = new AtomicReference<StartService>();
			var startCode = new long[]{-1};
			var tStop = new Thread(() -> {
				var r = stopReq(true);
				try {
					sm.stopService(r); // force：destroyForcibly(延迟1.5s真杀)+waitFor——窗口=摘账起到1.5s
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				stopReceipt.set(r);
			}, "t-stop");
			var tStart = new Thread(() -> {
				try {
					Thread.sleep(300); // 落进停机窗口（摘账已发生、真杀未发生）
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				var r = startReq();
				startCode[0] = sm.startService(r);
				startReceipt.set(r);
			}, "t-start");
			tStop.start();
			tStart.start();
			tStop.join(30_000);
			tStart.join(30_000);
			assertFalse(tStop.isAlive(), "stop 应有界完成");
			assertFalse(tStart.isAlive(), "start 应有界完成（互斥等待亦不得无界）");

			assertEquals(ServiceManager.STATE_FORCE_KILLED, stopReceipt.get().Result.getState(),
					"stop 正常收殓慢死进程");
			assertEquals(0, startCode[0], "start 应成功");
			assertEquals(ServiceManager.STATE_RUNNING, startReceipt.get().Result.getState());

			// 终局不变式（GE-C02 本案）：start 回执 Running ⇒ 服务真活着。
			// 修复前红：领养条目随进程死亡被 watchExit 收殓 → 条目 null；或收殓未及 → isAlive=false。
			Thread.sleep(300); // 收殓回调窗口
			var entry = sm.getProcessForTest("svc");
			assertTrue(null != entry && entry.isAlive(),
					"start 回执 Running 的服务必须真活着（不得领养正在被终止的进程）: entry=" + entry);

			// 收尾：停掉修复路径重新拉起的进程（receipt 断言+条目收殓有界等待）
			var stop = stopReq(true);
			sm.stopService(stop);
			assertEquals(ServiceManager.STATE_FORCE_KILLED, stop.Result.getState());
			for (var i = 0; i < 100 && null != sm.getProcessForTest("svc"); i++)
				Thread.sleep(50);
			assertNull(sm.getProcessForTest("svc"), "收尾停止后记账条目收殓");
		} finally {
			real.destroyForcibly();
		}
	}

	/** 持锁结构判别（全平台）：外部持有锁对象时 start/stop 都必须阻塞（全程持锁的直接验证）；
	 * 释放后都有界完成。修复前红：无 opsLocks 字段（getDeclaredField 抛 NoSuchFieldException）。 */
	@Test
	public void testStartAndStopHoldOpsLock() throws Exception {
		var servicesDir = servicesDir(tempDir);
		var sm = new ServiceManager(servicesDir);
		assertEquals(NO_PROPS, sm.startService(startReq()), "无布局→错误码（锁内路径）");

		var locks = opsLocksOf(sm);
		var lock = locks.get("svc");
		assertNotNull(lock, "锁条目以折叠键落账");
		var doneStart = new boolean[]{false};
		var doneStop = new boolean[]{false};
		var tStart = new Thread(() -> doneStart[0] = sm.startService(startReq()) == NO_PROPS, "hold-start");
		var tStop = new Thread(() -> {
			try {
				sm.stopService(stopReq(true)); // 无条目+无身份→not-running（一旦拿到锁即返回）
				doneStop[0] = true;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, "hold-stop");
		synchronized (lock) {
			tStart.start();
			tStop.start();
			Thread.sleep(400); // 锁被测试线程持有：两操作都不得进入
			assertFalse(doneStart[0], "start 必须全程持 opsLock（锁被持时阻塞）");
			assertFalse(doneStop[0], "stop 必须全程持 opsLock（锁被持时阻塞）");
		}
		tStart.join(10_000);
		tStop.join(10_000);
		assertTrue(doneStart[0], "释放后 start 有界完成");
		assertTrue(doneStop[0], "释放后 stop 有界完成");
	}

	/** 锁键大小写折叠（全平台）："svc"/"Svc" 同一物理容器必须同一把锁（TestFnd21E02 同论证）。 */
	@Test
	public void testOpsLocksCaseFolding() throws Exception {
		var servicesDir = servicesDir(tempDir);
		var sm = new ServiceManager(servicesDir);
		assertEquals(NO_PROPS, sm.startService(startReq()));
		var upper = new StartService();
		upper.Argument.setServiceName("Svc");
		assertEquals(NO_PROPS, sm.startService(upper));
		assertEquals(1, opsLocksOf(sm).size(), "大小写变体必须折叠到同一锁条目");
	}
}
