package Zeze.Onz;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import Zeze.Builtin.Onz.BSavedCommits;
import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Net.ServiceConf;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManagerServer;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import harness.Fast;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND22 GC-C01 回归：OnzServer.stop() 与在飞 perform 的库写路径无互斥。
 * perform 的写库点（saveCommitPoint/removeCommitRecord，含 OnzTransaction.commit 内两处）
 * 原先跑在业务线程、不持 dbLock 也不复查 stopped——perform 的业务长窗口
 * （txn.perform() 时长无上界）期间 stop() 可走完整个关库链（database.close 释放列族与
 * db 句柄），此后写点经 Table.put 进入 JNI 对已释放句柄做 native 写——正是
 * RocksDatabase.close 契约声明的 use-after-free（进程崩溃，perform 的 Java catch 救不了
 * native 崩溃）。dbLock 的引入注释自认"直接关库会与写入commitPoint竞态"却只对 redo 轮次
 * 设防；远程路径（ProcessFuncProcedureRequest 派发线程跑 perform，Service.stop 不 join
 * 在飞 handler）同样暴露。
 * 修复（对齐 redoTimer/settleStuckRecord 的锁内双检形态）：写点纳入 dbLock 域 + 锁内
 * stopped 复查——互斥只覆盖毫秒级写库操作（不含业务窗口），写要么先于 close 完成、要么
 * 在锁内看到 stopped 拒写；拒写把停机时在飞事务转为显式失败（perform/commit 的既有
 * catch→rollback 链返回 Procedure.Exception——"在途事务可能失败"的 stop javadoc 语义）。
 * <p>
 * 直构形态（@Fast，serverId 910/912 段、SM 端口 51910/51911）：进程内 ServiceManagerServer
 * + 两参构造器协调者（零桩参与方——本案只涉协调者侧写点，业务事务不调远程过程）。
 * 修复前红测形态：写点对已关库的 native 写（JNI 崩溃或 RocksDBException），见各断言注释。
 */
@Fast
public class TestFnd22GcC01StopPerformMutex {
	private Logger onzServerLogger;
	private CapturingAppender appender;

	@BeforeEach
	public void before() {
		onzServerLogger = (Logger)LogManager.getLogger(OnzServer.class);
		appender = new CapturingAppender();
		appender.start();
		onzServerLogger.addAppender(appender);
	}

	@AfterEach
	public void after() {
		if (onzServerLogger != null && appender != null)
			onzServerLogger.removeAppender(appender);
		if (appender != null)
			appender.stop();
	}

	/**
	 * 核心场景（案卷机制链的时序复刻）：业务线程进入 perform（入口单检 stopped 通过）→
	 * txn.perform() 无上界业务窗口；停机线程走完 stop()（关库）；业务返回后写
	 * ePreparing。修复后：saveCommitPoint 在 dbLock 内看到 stopped 拒写（拒写消息留痕），
	 * perform 走既有 catch→rollback 链显式失败返回 Procedure.Exception——允许失败、
	 * 不允许崩。修复前：写点对已释放句柄做 native 写（JNI 直接崩溃；即使 JNI 以
	 * RocksDBException 形式抛出，日志链中也不会出现拒写消息——断言两锚点任一为假即红）。
	 */
	@Test
	@Timeout(90)
	public void testInFlightPerformFailsCleanlyAcrossStop(@TempDir Path tempDir) throws Exception {
		try (var fixture = buildServer(910, 51910, 911, tempDir)) {
			var started = new CountDownLatch(1);
			var release = new CountDownLatch(1);
			var txn = new BlockedBusinessTxn(started, release);
			txn.setOnzServer(fixture.onzServer);

			var rc = new AtomicLong(Long.MIN_VALUE);
			var threadError = new AtomicReference<Throwable>();
			var performer = new Thread(() -> {
				try {
					rc.set(fixture.onzServer.perform(txn));
				} catch (Throwable t) {
					threadError.set(t); // perform 契约：不向调用方抛（catch 全兜底）
				}
			}, "fnd22gc01-performer");
			performer.start();
			Assertions.assertTrue(started.await(10, TimeUnit.SECONDS), "perform 必须已进入业务窗口");

			fixture.onzServer.stop(); // 停机线程：置位→…→database.close()，全程perform在飞
			release.countDown(); // 业务在关库完成后才返回——案卷的赛跑时序

			performer.join(30_000);
			Assertions.assertFalse(performer.isAlive(), "perform 必须有限时间返回（不允许挂死）");
			Assertions.assertNull(threadError.get(), "perform 不得向调用方抛异常");
			Assertions.assertEquals(Procedure.Exception, rc.get(),
					"停机时在飞事务必须显式失败（Procedure.Exception），不是崩溃也不是成功");
			Assertions.assertTrue(appender.hasErrorThrownContaining("OnzServer stopped: saveCommitPoint rejected"),
					"失败成因必须是写点拒写（锁内stopped复查），不是对已关库的native写");
		}
	}

	/**
	 * 写点契约直构（红测载体，确定性）：stop 完成后写点不得再触碰已关的库——
	 * saveCommitPoint 必须以显式 RuntimeException 拒写（对齐 getZezeInstance 的 stopped
	 * 拒绝形态）；removeCommitRecord 静默拒删（与既有 RocksDBException 分支同语义：记录
	 * 留库由下次启动 redo 恢复——stop javadoc 既定承诺）。
	 * 修复前：saveCommitPoint 对已 close 的库做 native put（JNI 崩溃=红；若 JNI 以
	 * RocksDBException 抛出，则 assertThrows(RuntimeException) 不匹配=红——两种结局皆红）。
	 */
	@Test
	@Timeout(90)
	public void testWritePointsRejectedAfterStopWithoutNativeTouch(@TempDir Path tempDir) throws Exception {
		try (var fixture = buildServer(912, 51911, 913, tempDir)) {
			fixture.onzServer.stop(); // 完整停机（关库）——此后写点只剩拒写一条路

			var tidBytes = new byte[8];
			ByteBuffer.longBeHandler.set(tidBytes, 0, 0xF22C01L);
			var state = new BSavedCommits.Data();

			var ex = Assertions.assertThrows(RuntimeException.class,
					() -> fixture.onzServer.saveCommitPoint(tidBytes, state, AbstractOnz.ePreparing),
					"stop后写点必须显式拒写（修复前：对已关库的native写）");
			Assertions.assertTrue(ex.getMessage().contains("stopped"), ex.getMessage());

			Assertions.assertDoesNotThrow(() -> fixture.onzServer.removeCommitRecord(tidBytes),
					"removeCommitRecord拒删是no-op不是错误（记录留库由重启redo恢复）");
			Assertions.assertTrue(appender.hasMessageContaining("removeCommitRecord rejected"),
					"拒删必须留痕（含tid），与RocksDBException分支同形态");
		}
	}

	// ---- 脚手架（对齐 Fnd21GcOnzFastSupport 形态，本类需 Zeze.Onz 包内直构写点故内联） ----

	/** 业务窗口可阻塞的桩事务：perform 先放行started，再等release（时长无上界的业务模拟）。 */
	static final class BlockedBusinessTxn extends OnzTransaction<EmptyBean.Data, EmptyBean.Data> {
		private final CountDownLatch started;
		private final CountDownLatch release;

		BlockedBusinessTxn(CountDownLatch started, CountDownLatch release) {
			this.started = started;
			this.release = release;
		}

		@Override
		protected long perform() throws Exception {
			started.countDown();
			release.await();
			return 0; // 业务成功——rc==0 正是修复前走到 saveCommitPoint 的路径
		}
	}

	record Fixture(ServiceManagerServer sm, OnzServer onzServer) implements AutoCloseable {
		@Override
		public void close() {
			try {
				onzServer.stop();
			} catch (Throwable ignore) {
				// best-effort：与OnzServer.stop自身的分步容错口径一致
			}
			try {
				sm.close();
			} catch (Throwable ignore) {
			}
		}
	}

	/** 进程内SM + 两参构造器协调者（单集群"名=配置"，零桩参与方）；半途失败best-effort回滚。 */
	private static Fixture buildServer(int serverId, int smPort, int clusterServerId, Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		Files.createDirectories(Path.of("autokeys"));
		deleteRecursively(Path.of("CommitOnzServer" + serverId));

		var clusterXml = tempDir.resolve("fnd22gc-cluster-" + serverId + ".xml");
		Files.writeString(clusterXml, """
				<?xml version="1.0" encoding="utf-8"?>
				<zeze ServerId="%d">
					<ServiceConf Name="Zeze.Services.ServiceManager.Agent">
						<Connector HostNameOrAddress="127.0.0.1" Port="%d"/>
					</ServiceConf>
				</zeze>
				""".formatted(clusterServerId, smPort));

		var sm = new ServiceManagerServer(null, smPort, new Config(), "autokeys/fnd22-gc-" + serverId);
		try {
			var myConfig = new Config();
			myConfig.setServerId(serverId);
			var sc = new ServiceConf();
			sc.addConnector(new Connector("127.0.0.1", smPort, false));
			myConfig.getServiceConfMap().put(Agent.defaultServiceName, sc);

			var onzServer = new OnzServer("fnd22zeze" + serverId + "=" + clusterXml, myConfig);
			onzServer.start();
			return new Fixture(sm, onzServer);
		} catch (Throwable ex) {
			sm.close();
			throw ex;
		}
	}

	private static void deleteRecursively(Path root) throws Exception {
		if (!Files.exists(root))
			return;
		try (var walk = Files.walk(root)) {
			walk.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.delete(p);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

	static final class CapturingAppender extends AbstractAppender {
		final List<LogEvent> events = new ArrayList<>();

		CapturingAppender() {
			super("aFnd22GcC01Capture", null, null, true, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(@NotNull LogEvent event) {
			synchronized (events) {
				events.add(event.toImmutable());
			}
		}

		boolean hasErrorThrownContaining(String fragment) {
			synchronized (events) {
				return events.stream().anyMatch(e -> {
					if (e.getLevel() != Level.ERROR)
						return false;
					var t = e.getThrown();
					while (t != null) {
						if (t.getMessage() != null && t.getMessage().toString().contains(fragment))
							return true;
						t = t.getCause();
					}
					return false;
				});
			}
		}

		boolean hasMessageContaining(String fragment) {
			synchronized (events) {
				return events.stream().anyMatch(e ->
						e.getMessage().getFormattedMessage().contains(fragment));
			}
		}
	}
}
