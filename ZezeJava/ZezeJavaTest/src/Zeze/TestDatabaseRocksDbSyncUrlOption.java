package Zeze;

import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import Zeze.Config;
import Zeze.Transaction.DatabaseRocksDb;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.rocksdb.WriteOptions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DatabaseRocksDb 写选项契约：DatabaseUrl 支持 ?sync=true（默认普通写），
 * query 在目录使用前剥离（homePath/.lock 文件名不能带'?'）。
 */
@Fast
public class TestDatabaseRocksDbSyncUrlOption {
	@TempDir
	Path tempDir;

	private Config.DatabaseConf rocksConf(String url) {
		var dbConf = new Config.DatabaseConf();
		dbConf.setName("sync-url-test");
		dbConf.setDatabaseType(Config.DbType.RocksDb);
		dbConf.setDatabaseUrl(url);
		return dbConf;
	}

	private static boolean effectiveSync(DatabaseRocksDb db) throws Exception {
		Field field = DatabaseRocksDb.class.getDeclaredField("writeOptions");
		field.setAccessible(true);
		return ((WriteOptions)field.get(db)).sync();
	}

	@Test
	public void plainUrlDefaultsToNormalWriteAndUsesBareDirectory() throws Exception {
		var dir = tempDir.resolve("plain");
		var db = new DatabaseRocksDb(null, rocksConf(dir.toString()));
		try {
			assertFalse(effectiveSync(db), "无选项必须默认普通写");
			assertTrue(Files.isDirectory(dir), "目录按裸路径创建");
		} finally {
			db.close();
		}
	}

	@Test
	public void syncTrueStripsQueryAndEnablesSyncWrite() throws Exception {
		var dir = tempDir.resolve("synced");
		var db = new DatabaseRocksDb(null, rocksConf(dir + "?sync=true"));
		try {
			assertTrue(effectiveSync(db), "?sync=true必须生效");
			// Windows路径禁'?'，带query的目录本就无法创建——裸目录存在即证明query已剥离。
			assertTrue(Files.isDirectory(dir), "query必须在使用前剥离——目录不能带'?'");
		} finally {
			db.close();
		}
	}
}
