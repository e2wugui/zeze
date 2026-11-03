package Zeze.Dbh2;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Config;
import Zeze.Dbh2.Master.Master;
import Zeze.Dbh2.Master.MasterDatabase;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestSplittingRegistrationKeepsCurrentGeneration {
	private static Object field(Object target, String name) throws Exception {
		var field = MasterDatabase.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}

	private static BBucketMeta.Data bucket(int port) {
		var bucket = new BBucketMeta.Data();
		bucket.setDatabaseName("db");
		bucket.setTableName("table");
		bucket.setRaftConfig("<raft Name=\"RaftName\"><node Host=\"127.0.0.1\" Port=\"" + port + "\"/></raft>");
		return bucket;
	}

	@SuppressWarnings("unchecked")
	@Test
	void delayedRegistrationCannotOverwriteReplacementAgeAndSource(@TempDir Path home) throws Exception {
		var master = new Master(home.toString(), new Config());
		var db = new MasterDatabase(master, "db");
		try {
			var source = bucket(19201);
			var oldTarget = bucket(19202);
			var replacement = bucket(19203);
			var main = new MasterTable.Data();
			main.getBuckets().put(Binary.Empty, source);
			db.getTables().put("table", main);
			var pending = new MasterTable.Data();
			pending.getBuckets().put(Binary.Empty, replacement);
			((ConcurrentHashMap<String, MasterTable.Data>)field(db, "splitting")).put("table", pending);
			var keyMethod = MasterDatabase.class.getDeclaredMethod("splittingAgeKey", String.class, Binary.class);
			keyMethod.setAccessible(true);
			var key = (byte[])keyMethod.invoke(null, "table", Binary.Empty);
			var value = ByteBuffer.Allocate();
			value.WriteLong(1L);
			value.WriteString("replacement-source");
			var ages = (RocksDatabase.Table)field(db, "rocksSplittingAge");
			ages.put(key, value.Copy());
			var register = MasterDatabase.class.getDeclaredMethod("registerSplittingGeneration",
					String.class, BBucketMeta.Data.class);
			register.setAccessible(true);
			register.invoke(db, "table", oldTarget);
			assertArrayEquals(value.Copy(), ages.get(key), "旧目标回调不得污染新目标的年龄与源身份");
			register.invoke(db, "table", replacement);
			assertTrue(db.getSplittingAgeCreateTime("table", Binary.Empty) > 1L);
			assertSame(replacement, pending.getBuckets().get(Binary.Empty));
		} finally {
			db.close();
			master.close();
		}
	}
}
