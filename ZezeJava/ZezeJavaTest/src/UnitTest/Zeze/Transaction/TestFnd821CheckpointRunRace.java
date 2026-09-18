package UnitTest.Zeze.Transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.CheckpointMode;
import Zeze.Transaction.Procedure;
import Zeze.Util.FuncLong;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND8-21 回归：checkpointRun对volatile字段checkpoint判空后二次读——stop()持
 * Application锁置null恰落在两条载入之间时，解引用得null即NPE。修复为单次快照读
 * （本地变量判空后解引用），停机窗口内调用要么no-op要么对快照执行runOnce。
 * 覆盖：stop完成后checkpointRun安全no-op；并发stop期间循环调用checkpointRun
 * 不得有异常逃逸（runOnce两种模式自身吞单元异常，唯一逃逸源就是双读NPE）。
 */
@Fast
public class TestFnd821CheckpointRunRace {
	// 独立serverId+url：@Fast类并行时避免本地RocksCache目录互撞。
	private static final int SERVER_ID = 12821;

	@TempDir
	Path tempDir;

	private Application app;

	@BeforeEach
	public void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setCheckpointMode(CheckpointMode.Table);
		conf.setServerId(SERVER_ID);
		conf.setDefaultTableConf(new Config.TableConf()); // 裸Config不会补默认值
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.RocksDb);
		dbConf.setDatabaseUrl(tempDir.resolve("dbhome").toString());
		conf.getDatabaseConfMap().put("", dbConf);
		app = new Application("TestFnd821CheckpointRunRace", conf);
		app.start();
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (app.getStartState() != Application.StartState.eStopped)
			app.stop(); // 已停实例幂等
	}

	/** stop完成后（checkpoint已置null）：checkpointRun必须是安全no-op，不得抛NPE。 */
	@Test
	public void testSafeNoopAfterStop() throws Exception {
		app.stop();
		assertEquals(Application.StartState.eStopped, app.getStartState());
		for (int i = 0; i < 1000; i++)
			assertDoesNotThrow(app::checkpointRun, "checkpoint==null时快照读必须安全跳过（不得判空后二次读字段）");
	}

	/**
	 * 并发stop期间高频调用checkpointRun：穿越"置null窗口"不得有异常逃逸。
	 * 线程循环至观察到eStopped后再跑1000轮（覆盖置null前后两侧），全程收集异常。
	 */
	@Test
	public void testNoEscapeAcrossStopWindow() throws Exception {
		var error = new AtomicReference<Throwable>();
		var loopDone = new CountDownLatch(1);
		var looper = Thread.ofPlatform().daemon().start(() -> {
			try {
				int afterStop = 0;
				while (afterStop < 1000) {
					app.checkpointRun(); // 唯一逃逸源：双读NPE（runOnce自身吞单元异常）
					if (app.getStartState() == Application.StartState.eStopped)
						afterStop++;
				}
			} catch (Throwable e) {
				error.set(e);
			} finally {
				loopDone.countDown();
			}
		});

		app.stop(); // 持Application锁置null——与looper的volatile读并发

		assertTrue(loopDone.await(30, TimeUnit.SECONDS), "循环必须在有界时间内完成");
		looper.join(10_000);
		assertFalse(looper.isAlive(), "循环线程必须结束");
		assertNull(error.get(), "checkpointRun穿越stop窗口不得抛异常（双读NPE）");
	}

	/** 行为不变：运行期checkpointRun正常返回（空map的runOnce为无害no-op）。 */
	@Test
	public void testNormalRunUnaffected() throws Exception {
		assertDoesNotThrow(app::checkpointRun);
		var rc = app.newProcedure((FuncLong)() -> 0L, "Fnd821.Nop").call();
		assertEquals(Procedure.Success, rc);
		assertDoesNotThrow(app::checkpointRun);
	}
}
