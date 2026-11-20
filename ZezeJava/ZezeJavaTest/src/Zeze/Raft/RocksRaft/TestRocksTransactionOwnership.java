package Zeze.Raft.RocksRaft;

import java.nio.file.Path;
import Zeze.Builtin.ServiceManagerWithRaft.BAutoKey;
import Zeze.Config;
import Zeze.Raft.RaftConfig;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
class TestRocksTransactionOwnership {
	@TempDir
	Path dbHome;

	// No servers are started, so the two stores can use the same node addresses.
	private Rocks open(String directory) throws Exception {
		var config = RaftConfig.loadFromString("""
				<raft Name="127.0.0.1:17801">
				    <node Host="127.0.0.1" Port="17801"/>
				    <node Host="127.0.0.1" Port="17802"/>
				    <node Host="127.0.0.1" Port="17803"/>
				</raft>
				""");
		config.setDbHome(dbHome.resolve(directory).toString());
		return new Rocks(null, RocksMode.Pessimism, config, new Config(), false);
	}

	@Test
	void rejectsForeignTransactionalWorkAndKeepsDirtyReadsInTheirStore() throws Exception {
		Task.tryInitThreadPool();
		try (var first = open("first"); var second = open("second")) {
			var firstTable = openTable(first);
			var secondTable = openTable(second);
			var stopWithoutAppend = 17L;
			assertEquals(stopWithoutAppend, first.newProcedure(() -> {
				var local = firstTable.getOrAdd(1);
				assertEquals(0, first.newProcedure(() -> {
					assertSame(local, firstTable.get(1));
					return 0;
				}).call());
				assertThrows(IllegalStateException.class, () -> second.newProcedure(() -> {
					throw new AssertionError("foreign procedure body ran");
				}).call());
				assertThrows(IllegalStateException.class, () -> secondTable.get(1));
				assertThrows(IllegalStateException.class, () -> secondTable.put(1, new BAutoKey()));
				assertSame(local, firstTable.selectDirty(1));
				assertNull(secondTable.selectDirty(1));
				return stopWithoutAppend;
			}).call());
		}
	}

	private static Table<Integer, BAutoKey> openTable(Rocks rocks) {
		rocks.registerTableTemplate("same", Integer.class, BAutoKey.class);
		return java.util.Objects.requireNonNull(rocks.<Integer, BAutoKey>getTableTemplate("same")).openTable();
	}
}
