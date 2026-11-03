package Zeze.Dbh2;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Master.EndMove;
import Zeze.Builtin.Dbh2.Master.EndSplit;
import Zeze.Config;
import Zeze.Dbh2.Master.Master;
import Zeze.Dbh2.Master.MasterDatabase;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.Net.Binary;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestSettleRequiresCurrentTarget {
	private static BBucketMeta.Data bucket(int first, int port) {
		var meta = new BBucketMeta.Data();
		meta.setDatabaseName("database");
		meta.setTableName("table");
		meta.setKeyFirst(new Binary(new byte[]{(byte)first}));
		meta.setRaftConfig("<raft Name=\"RaftName\"><node Host=\"127.0.0.1\" Port=\"" + port + "\"/></raft>");
		return meta;
	}

	@SuppressWarnings("unchecked")
	private static MasterTable.Data splitting(MasterDatabase db) throws Exception {
		var field = MasterDatabase.class.getDeclaredField("splitting");
		field.setAccessible(true);
		return ((ConcurrentHashMap<String, MasterTable.Data>)field.get(db))
				.computeIfAbsent("table", __ -> new MasterTable.Data());
	}

	@Test
	void lateMoveCannotConsumeReplacementTarget(@TempDir Path home) throws Exception {
		verifyTargetGeneration(home, false);
	}

	@Test
	void lateSplitCannotConsumeReplacementTarget(@TempDir Path home) throws Exception {
		verifyTargetGeneration(home, true);
	}

	private static void verifyTargetGeneration(Path home, boolean split) throws Exception {
		var master = new Master(home.toString(), new Config());
		var db = new MasterDatabase(master, "database");
		try {
			var source = bucket(1, 19101);
			var oldTarget = bucket(split ? 5 : 1, 19102);
			var replacement = bucket(split ? 5 : 1, 19103);
			var main = new MasterTable.Data();
			main.getBuckets().put(source.getKeyFirst(), source);
			db.getTables().put("table", main);
			var pending = splitting(db);
			pending.getBuckets().put(replacement.getKeyFirst(), replacement);
			var from = source.copy();
			from.setKeyLast(oldTarget.getKeyFirst());
			long rc;
			if (split) {
				var request = new EndSplit();
				request.Argument.setFrom(from);
				request.Argument.setTo(oldTarget);
				rc = db.endSplit(request);
			} else {
				var request = new EndMove();
				request.Argument.setTo(oldTarget);
				rc = db.endMove(request);
			}
			assertEquals(master.errorCode(Master.eSplittingBucketNotFound), rc);
			assertEquals(1, main.getBuckets().size());
			assertSame(source, main.getBuckets().get(source.getKeyFirst()));
			assertSame(replacement, pending.getBuckets().get(replacement.getKeyFirst()));

			// 现行目标的正常结算仍须发布并消费，拒绝旧代不能阻断新代。
			if (split) {
				var request = new EndSplit();
				request.Argument.setFrom(from);
				request.Argument.setTo(replacement);
				assertEquals(0, db.endSplit(request));
			} else {
				var request = new EndMove();
				request.Argument.setTo(replacement);
				assertEquals(0, db.endMove(request));
			}
			assertSame(replacement, main.getBuckets().get(replacement.getKeyFirst()));
			assertTrue(pending.getBuckets().isEmpty());
		} finally {
			db.close();
			master.close();
		}
	}
}
