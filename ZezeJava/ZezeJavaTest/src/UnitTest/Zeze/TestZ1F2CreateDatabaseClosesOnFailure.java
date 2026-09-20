package UnitTest.Zeze;

import java.nio.file.Path;
import java.util.HashMap;

import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.Database;
import Zeze.Transaction.DatabaseRocksDb;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Z1-F2回归：createDatabase(Application, HashMap)逐库构造无失败清理——先建库的
 * 原生句柄/LOCK随半成品Application不可达而泄漏，进程内重试将永久
 * "lock held by current process"。修复：循环套try/catch，失败时逐个close已放入
 * map的Database再重抛（与clearInUseAndIAmSureAppStopped的finally-close形态对齐）。
 * 可观测性用RocksDB目录锁：close释放目录锁——修复前"ok"库泄漏（同目录再open必抛），
 * 修复后失败路径把它关掉，同目录可再open。
 */
@Fast
public class TestZ1F2CreateDatabaseClosesOnFailure {
	// 独立serverId+url：@Fast类并行时避免本地RocksCache目录互撞。
	private static final int SERVER_ID = 12821;

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
		dbConf.setDatabaseUrl("z1f2_memory");
		conf.getDatabaseConfMap().put("", dbConf);
		app = new Application("TestZ1F2CreateDatabaseClosesOnFailure", conf);
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

	/** 逐库构造中途失败：已放入map的Database必须被close（目录锁释放，同目录可再开）。 */
	@Test
	public void testPartialFailureClosesCreatedDatabases() throws Exception {
		// 外部持有的锁：占住"fail"目录，使createDatabase在它那里失败。
		var heldDir = tempDir.resolve("z1f2_held");
		var holder = new DatabaseRocksDb(app, rocksConf("fail", heldDir), false);
		try {
			var okDir = tempDir.resolve("z1f2_ok");
			var target = new Config();
			target.setDefaultTableConf(new Config.TableConf());
			target.getDatabaseConfMap().put("ok", rocksConf("ok", okDir));
			target.getDatabaseConfMap().put("fail", rocksConf("fail", heldDir));

			var map = new HashMap<String, Database>();
			// "fail"的open必抛（目录LOCK按句柄计）——createDatabase中途失败，异常原样重抛。
			assertThrows(Exception.class, () -> target.createDatabase(app, map),
					"被占目录的open必须失败且异常重抛");

			// 修复前："ok"已建实例随异常路径泄漏（目录锁滞留，无法再开）；
			// 修复后：失败清理把"ok"关掉，目录可再开。
			var okAgain = new DatabaseRocksDb(app, rocksConf("ok", okDir), false);
			okAgain.close(); // open成功即证明锁已释放
		} finally {
			holder.close();
		}
	}

	/** 全部成功：不关闭（所有权移交调用方，Application.stop自会关），行为不回归。 */
	@Test
	public void testSuccessDoesNotClose() throws Exception {
		var okDir = tempDir.resolve("z1f2_ok2");
		var target = new Config();
		target.setDefaultTableConf(new Config.TableConf());
		target.getDatabaseConfMap().put("ok2", rocksConf("ok2", okDir));

		var map = new HashMap<String, Database>();
		target.createDatabase(app, map);
		var created = map.get("ok2");
		assertTrue(created != null, "成功路径必须把建好的Database交给调用方");

		// 同目录再open必须失败（锁仍被map中的实例持有）——证明成功路径未误关。
		assertThrows(Exception.class,
				() -> new DatabaseRocksDb(app, rocksConf("ok2", okDir), false),
				"成功路径锁未释放，同目录open必须冲突");
		created.close();
	}
}
