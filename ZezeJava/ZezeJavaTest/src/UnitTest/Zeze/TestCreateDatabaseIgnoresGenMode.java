package UnitTest.Zeze;

import java.nio.file.Path;

import Zeze.Application;
import Zeze.Arch.Gen.GenModule;
import Zeze.Config;
import Zeze.Transaction.DatabaseRocksDb;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 生成模式与建库解耦的回归：createDatabase不再读genFileSrcRoot（原三元式在生成模式下
 * 把所有后端强制换成Memory）——生成模式由入口负责根本不建库（Game.App的Start在
 * createZeze前直接createRedirectModules生成并退出），工厂永远按conf真实类型建库。
 * 用RocksDb观测：修复前genFileSrcRoot置位时返回DatabaseMemory（红），修复后返回
 * DatabaseRocksDb（绿，真实开目录锁，测试里用完即关）。
 */
@Fast
public class TestCreateDatabaseIgnoresGenMode {
	// 独立serverId：@Fast类并行时避免本地RocksCache目录互撞。
	private static final int SERVER_ID = 12831;

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
		dbConf.setDatabaseUrl("a2_genmode_memory");
		conf.getDatabaseConfMap().put("", dbConf);
		app = new Application("TestCreateDatabaseIgnoresGenMode", conf);
		app.start();
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (app.getStartState() != Application.StartState.eStopped)
			app.stop();
	}

	@Test
	public void testFactoryIgnoresGenFileSrcRoot() throws Exception {
		var rocksConf = new Config.DatabaseConf();
		rocksConf.setName("genmode_rocks");
		rocksConf.setDatabaseType(Config.DbType.RocksDb);
		rocksConf.setDatabaseUrl(tempDir.resolve("rocks").toString());
		GenModule.instance.genFileSrcRoot = tempDir.toString();
		try {
			var db = Config.createDatabase(app, rocksConf);
			try {
				Assertions.assertInstanceOf(DatabaseRocksDb.class, db, "工厂不得受生成模式影响（生成模式由Start的gen分支负责根本不建库）: " + db.getClass());
			} finally {
				db.close();
			}
		} finally {
			GenModule.instance.genFileSrcRoot = null; // JVM级全局开关必须恢复
		}
	}
}
