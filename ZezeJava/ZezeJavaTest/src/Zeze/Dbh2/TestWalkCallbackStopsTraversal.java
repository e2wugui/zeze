package Zeze.Dbh2;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BWalkKeyValue;
import Zeze.Builtin.Dbh2.Walk;
import Zeze.Builtin.Dbh2.WalkKey;
import Zeze.Config;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Fast
public class TestWalkCallbackStopsTraversal {

	// RPC replies are stubbed; the real manager still locates both buckets and
	// delivers their pages. No acceptor or external service is needed.
	private static final class PageAgent extends Dbh2Agent {
		int requests;

		PageAgent() throws Exception {
			super("<raft Name=\"\"><node Host=\"127.0.0.1\" Port=\"1\"/></raft>");
		}

		private List<Binary> page(boolean desc, Binary first) {
			requests++;
			return first.size() == 0
					? desc ? List.of(key(2), key(1)) : List.of(key(1), key(2))
					: desc ? List.of(key(4), key(3)) : List.of(key(3), key(4));
		}

		@Override
		public Walk walk(Binary exclusive, int limit, boolean desc, byte[] prefix,
				Binary expectedFirst, Binary expectedLast) {
			var reply = new Walk();
			for (var key : page(desc, expectedFirst))
				reply.Result.getKeyValues().add(new BWalkKeyValue.Data(key, key));
			reply.Result.setBucketEnd(true);
			return reply;
		}

		@Override
		public WalkKey walkKey(Binary exclusive, int limit, boolean desc, byte[] prefix,
				Binary expectedFirst, Binary expectedLast) {
			var reply = new WalkKey();
			reply.Result.getKeys().addAll(page(desc, expectedFirst));
			reply.Result.setBucketEnd(true);
			return reply;
		}
	}

	private static Binary key(int value) {
		return new Binary(new byte[]{(byte)value});
	}

	private static Dbh2AgentManager manager(Path tempDir, PageAgent agent) throws Exception {
		Task.tryInitThreadPool();
		var manager = new Dbh2AgentManager(new Dbh2AgentStubSupport.NullServiceAgent(),
				Config.load(Dbh2AgentStubSupport.writeRemoteCommitConfig(tempDir).toString())) {
			@Override
			public Dbh2Agent openBucket(String raft) {
				return agent;
			}
		};
		var table = new MasterTable.Data();
		for (var first : List.of(Binary.Empty, key(3))) {
			var meta = new BBucketMeta.Data();
			meta.setKeyFirst(first);
			meta.setKeyLast(first.size() == 0 ? key(3) : Binary.Empty);
			meta.setRaftConfig("stub");
			table.getBuckets().put(first, meta);
		}
		manager.putBuckets(table, "master", "database", "table");
		return manager;
	}

	@Test
	void fullWalkStopsAndCountsTheDeliveredRecord(@TempDir Path tempDir) throws Exception {
		checkFullWalk(tempDir, false);
	}

	@Test
	void fullWalkKeyStopsAndCountsTheDeliveredRecord(@TempDir Path tempDir) throws Exception {
		checkFullWalk(tempDir, true);
	}

	private static void checkFullWalk(Path tempDir, boolean keysOnly) throws Exception {
		var agent = new PageAgent();
		var manager = manager(tempDir, agent);
		try {
			for (var desc : List.of(false, true)) {
				agent.requests = 0;
				var seen = new ArrayList<Binary>();
				var count = keysOnly
						? manager.walkKey(null, "master", "database", "table", k -> {
							seen.add(new Binary(k));
							return false;
						}, desc, null)
						: manager.walk(null, "master", "database", "table", (k, v) -> {
							seen.add(new Binary(k));
							return false;
						}, desc, null);
				assertEquals(List.of(key(desc ? 4 : 1)), seen);
				assertEquals(1, count);
				assertEquals(1, agent.requests, "Stopping must not fetch the next bucket");
			}
		} finally {
			manager.stop();
			agent.close();
		}
	}

	@Test
	void pagedWalkReturnsTheStoppingRecordAsCursor(@TempDir Path tempDir) throws Exception {
		checkPagedWalk(tempDir, false);
	}

	@Test
	void pagedWalkKeyReturnsTheStoppingRecordAsCursor(@TempDir Path tempDir) throws Exception {
		checkPagedWalk(tempDir, true);
	}

	private static void checkPagedWalk(Path tempDir, boolean keysOnly) throws Exception {
		var agent = new PageAgent();
		var manager = manager(tempDir, agent);
		try {
			for (var desc : List.of(false, true)) {
				agent.requests = 0;
				var seen = new ArrayList<Binary>();
				ByteBuffer cursor = keysOnly
						? manager.walkKey(null, "master", "database", "table", null, 10, k -> {
							seen.add(new Binary(k));
							return false;
						}, desc, null)
						: manager.walk(null, "master", "database", "table", null, 10, (k, v) -> {
							seen.add(new Binary(k));
							return false;
						}, desc, null);
				assertEquals(List.of(key(desc ? 4 : 1)), seen);
				assertNotNull(cursor);
				assertEquals(key(desc ? 4 : 1), new Binary(cursor));
				assertEquals(1, agent.requests, "Stopping must not fetch the next bucket");
			}
		} finally {
			manager.stop();
			agent.close();
		}
	}
}
