package UnitTest.Zeze;

import java.nio.file.Path;

import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.DatabaseRocksDb;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FND8-24 回归：clearInUseAndIAmSureAppStopped(独立建库形态)自建整批Database（连接池/
 * RocksDB句柄）用完从不close。修复：增量建图+finally逐db异常隔离地close（建一半
 * 炸了也关已建的）；调用方自有实例走clearInUse(map)，所有权在调用方、维持不关。
 * 可观测性用RocksDB目录锁：DatabaseRocksDb.close释放目录锁——修复前自建实例
 * 不关，同目录第二次open必抛RocksDBException（目录LOCK按句柄计，同进程同样冲突）；
 * 修复后随调用粒度释放，第二次open成功。
 */
@Fast
public class TestFnd824ClearInUseClosesCreatedDatabases {
	// 独立serverId+url：@Fast类并行时避免本地RocksCache目录互撞。
	private static final int SERVER_ID = 12824;

	@TempDir
	Path tempDir;

	private Application app;

	@BeforeEach
	public void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(SERVER_ID);
		conf.setDefaultTableConf(new Config.TableConf()); // 裸Config不会补默认值
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("a2_fnd824_memory");
		conf.getDatabaseConfMap().put("", dbConf);
		app = new Application("TestFnd824ClearInUseClosesCreatedDatabases", conf);
		app.start();
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (app.getStartState() != Application.StartState.eStopped)
			app.stop(); // 已停实例幂等
	}

	private Config.DatabaseConf rocksConf(String name, Path dir) {
		var dbConf = new Config.DatabaseConf();
		dbConf.setName(name);
		dbConf.setDatabaseType(Config.DbType.RocksDb);
		dbConf.setDatabaseUrl(dir.toString());
		return dbConf;
	}

	/** 独立建库形态自建的RocksDb在调用结束后必须已close（目录锁释放，同目录可再开）。 */
	@Test
	public void testStandaloneClosesCreatedRocksDb() throws Exception {
		var target = new Config();
		target.setDefaultTableConf(new Config.TableConf());
		target.getDatabaseConfMap().put("a2db", rocksConf("a2db", tempDir.resolve("a2db")));

		// 修复前：自建实例不关，目录锁滞留 → 第二次open抛RocksDBException（红）。
		// 修复后：finally关闭 → 目录锁随调用粒度释放 → 第二次open成功（绿）。
		target.clearInUseAndIAmSureAppStopped(app);

		var again = new DatabaseRocksDb(app, rocksConf("a2db", tempDir.resolve("a2db")), false);
		again.close(); // open成功即证明锁已释放
	}

	/** 建一半失败（第二个后端目录被外部持有）：已建实例也必须在finally被关。 */
	@Test
	public void testPartialBuildFailureClosesCreatedBatch() throws Exception {
		// 外部持有的锁：占住"fail"目录，使createDatabase在它那里失败。
		var heldDir = tempDir.resolve("a2_held");
		var holder = new DatabaseRocksDb(app, rocksConf("fail", heldDir), false);
		try {
			var okDir = tempDir.resolve("a2_ok");
			var target = new Config();
			target.setDefaultTableConf(new Config.TableConf());
			target.getDatabaseConfMap().put("ok", rocksConf("ok", okDir));
			target.getDatabaseConfMap().put("fail", rocksConf("fail", heldDir));

			// "fail"的open必抛（目录LOCK按句柄计）——createDatabase中途失败。
			assertThrows(Exception.class, () -> target.clearInUseAndIAmSureAppStopped(app),
					"被占目录的open必须失败");

			// 修复前："ok"已建实例随异常路径泄漏（目录锁滞留，无法再开）；
			// 修复后：增量建图+finally把已建的"ok"关掉，目录可再开。
			var okAgain = new DatabaseRocksDb(app, rocksConf("ok", okDir), false);
			okAgain.close(); // open成功即证明锁已释放
		} finally {
			holder.close();
		}
	}

	/** 调用方自有实例走clearInUse(map)：所有权在调用方，方法不得关闭（app停机序列自会关）。 */
	@Test
	public void testCallerOwnedDatabasesNotClosed() {
		var conf = new Config();
		conf.setDefaultTableConf(new Config.TableConf());
		assertDoesNotThrow(() -> conf.clearInUse(app.getDatabases()));
		// app的库仍可用（未被动过）：正常事务照常执行。
		assertDoesNotThrow(() -> app.newProcedure(() -> 0L, "Fnd824.StillAlive").call());
	}
}
