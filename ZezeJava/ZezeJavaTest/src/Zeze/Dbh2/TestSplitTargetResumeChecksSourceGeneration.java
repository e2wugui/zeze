package Zeze.Dbh2;

import harness.Extra;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Master.CreateSplitBucket;
import Zeze.Config;
import Zeze.Dbh2.Master.Master;
import Zeze.Dbh2.Master.MasterDatabase;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.IModule;
import Zeze.Net.Binary;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Fast
@Extra
public class TestSplitTargetResumeChecksSourceGeneration {
	private static Binary key(int value) {
		return new Binary(new byte[]{(byte)value});
	}

	private static BBucketMeta.Data meta(Binary first, Binary last, int identity) {
		var meta = new BBucketMeta.Data();
		meta.setDatabaseName("database");
		meta.setTableName("table");
		meta.setKeyFirst(first);
		meta.setKeyLast(last);
		meta.setRaftConfig("<raft Name=\"\"><node Host=\"127.0.0.1\" Port=\"" + identity + "\"/></raft>");
		return meta;
	}

	@SuppressWarnings("unchecked")
	private static <V> ConcurrentHashMap<String, V> map(Object owner, String name) throws Exception {
		var field = owner.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return (ConcurrentHashMap<String, V>)field.get(owner);
	}

	private static void checkResume(Path tempDir, boolean sourceChanged) throws Exception {
		Files.createDirectories(tempDir.resolve("database"));
		var master = new Master(tempDir.toString(), new Config());
		try {
			var database = TestSplitTargetResumeChecksSourceGeneration.<MasterDatabase>map(master, "databases")
					.get("database");
			var main = new MasterTable.Data();
			main.getBuckets().put(Binary.Empty, meta(Binary.Empty, Binary.Empty, 1));
			database.getTables().put("table", main);
			var oldTarget = meta(key(4), Binary.Empty, 4);
			var splitting = new MasterTable.Data();
			splitting.getBuckets().put(key(4), oldTarget);
			TestSplitTargetResumeChecksSourceGeneration.<MasterTable.Data>map(database, "splitting")
					.put("table", splitting);
			var register = MasterDatabase.class.getDeclaredMethod("registerSplittingGeneration",
					String.class, BBucketMeta.Data.class);
			register.setAccessible(true);
			register.invoke(database, "table", oldTarget);

			if (sourceChanged) {
				// The original create(M=4) was abandoned before LogSetSplittingMeta.
				// A later split at M=2 completed; its new right bucket now requests M=4.
				main.getBuckets().put(Binary.Empty, meta(Binary.Empty, key(2), 1));
				main.getBuckets().put(key(2), meta(key(2), Binary.Empty, 2));
			}
			var request = new CreateSplitBucket();
			request.Argument.assign(oldTarget);
			request.Argument.setRaftConfig("");
			var result = database.createSplitBucket(request);
			assertEquals(sourceChanged ? Master.eSplittingBucketExist : 0, IModule.getErrorCode(result),
					"A target owned by an obsolete source must await recycling, not become the new source's copy target");
			assertEquals(oldTarget, splitting.getBuckets().get(key(4)),
					"The old target remains registered until the existing orphan recycler removes its raft");
			if (!sourceChanged)
				assertEquals(oldTarget.getRaftConfig(), request.Result.getRaftConfig());
		} finally {
			master.close();
		}
	}

	@Test
	void obsoleteSourceTargetIsNotResumedByAnotherBucket(@TempDir Path tempDir) throws Exception {
		checkResume(tempDir, true);
	}

	@Test
	void sameSourceTargetStillResumesIdempotently(@TempDir Path tempDir) throws Exception {
		checkResume(tempDir, false);
	}
}
