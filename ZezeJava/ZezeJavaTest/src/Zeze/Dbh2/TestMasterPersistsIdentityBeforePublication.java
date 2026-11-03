package Zeze.Dbh2;

import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import Zeze.Config;
import Zeze.Dbh2.Master.Master;
import Zeze.Dbh2.Master.MasterDatabase;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.Util.RocksDatabase;
import Zeze.Serialize.ByteBuffer;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestMasterPersistsIdentityBeforePublication {
	private static long walSyncs(RocksDatabase database) throws Exception {
		var stats = database.getRocksDb().getProperty("rocksdb.stats");
		var matcher = Pattern.compile("Cumulative WAL:.*?([0-9]+) syncs").matcher(stats);
		assertTrue(matcher.find(), stats);
		return Long.parseLong(matcher.group(1));
	}

	private static Object field(Master master, String name) throws Exception {
		var field = Master.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(master);
	}

	@Test
	void allocatedBucketIdentityUsesSynchronousWal(@TempDir Path home) throws Exception {
		var master = new Master(home.toString(), new Config());
		try {
			var database = (RocksDatabase)field(master, "masterDb");
			var before = walSyncs(database);
			var allocated = master.nextBucketPortId("manager");
			assertTrue(walSyncs(database) > before, "身份返回前必须完成一次真实 WAL sync");
			var seed = (RocksDatabase.Table)field(master, "seedTable");
			assertEquals(allocated, ByteBuffer.Wrap(seed.get("manager".getBytes(StandardCharsets.UTF_8))).ReadInt());
		} finally {
			master.close();
		}
	}

	@Test
	void publishedTableMetadataUsesSynchronousWal(@TempDir Path home) throws Exception {
		try (var database = new RocksDatabase(home.resolve("metadata").toString())) {
			var table = database.getOrAddTable("tables");
			var save = MasterDatabase.class.getDeclaredMethod("saveRocks", RocksDatabase.Table.class,
					String.class, MasterTable.Data.class);
			save.setAccessible(true);
			var metadata = new MasterTable.Data();
			var before = walSyncs(database);
			save.invoke(null, table, "table", metadata);
			assertTrue(walSyncs(database) > before, "元数据发布前必须完成一次真实 WAL sync");
			assertArrayEquals(metadata.encode().Copy(), table.get("table".getBytes(StandardCharsets.UTF_8)));
		}
	}
}
