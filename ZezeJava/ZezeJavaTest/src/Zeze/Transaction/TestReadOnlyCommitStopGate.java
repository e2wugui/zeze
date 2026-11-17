package Zeze.Transaction;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SQLStatement;
import harness.Fast;
import harness.FastServerIds;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

@Fast
public class TestReadOnlyCommitStopGate {
	@Test
	void emptyAndReadOnlyTransactionsRespectAClosedCommitGate() throws Exception {
		for (var mode : CheckpointMode.values()) {
			var config = new Config();
			config.setServiceManager("disable");
			config.setServerId(FastServerIds.TEST_READ_ONLY_COMMIT_STOP_GATE);
			config.setCheckpointMode(mode);
			config.setCheckpointPeriod(3_600_000);
			config.setDefaultTableConf(new Config.TableConf());
			var database = new Config.DatabaseConf();
			database.setDatabaseType(Config.DbType.Memory);
			database.setDatabaseUrl("TestReadOnlyCommitStopGate_" + config.getServerId() + '_' + mode);
			config.getDatabaseConfMap().put("", database);
			var app = new Application("TestReadOnlyCommitStopGate", config);
			var table = new ValueTable();
			app.addTable("", table);
			try {
				app.start();
				// Keep the checkpoint reference published, as for an in-flight commit that has
				// already captured it when shutdown closes its gate.
				requireNonNull(app.getCheckpoint()).stopAndJoin();
				for (boolean read : new boolean[]{false, true}) {
					var commits = new AtomicInteger();
					var rollbacks = new AtomicInteger();
					assertEquals(Procedure.Closed, app.newProcedure(() -> {
						if (read)
							table.get(1L);
						Transaction.whileCommit(commits::incrementAndGet);
						Transaction.whileRollback(rollbacks::incrementAndGet);
						return Procedure.Success;
					}, "ReadOnlyCommitStopGate").call(), mode + " read=" + read);
					assertEquals(0, commits.get());
					assertEquals(1, rollbacks.get());
				}
			} finally {
				app.stop();
			}
		}
	}

	public static final class Value extends Bean {
		@Override public void encode(ByteBuffer bb) { bb.WriteByte(0); }
		@Override public void decode(IByteBuffer bb) { bb.SkipUnknownField(ByteBuffer.BEAN); }
	}

	private static final class ValueTable extends TableX<Long, Value> {
		ValueTable() { super(991901, "TestReadOnlyCommitStopGate_values"); }
		@Override public Class<Long> getKeyClass() { return Long.class; }
		@Override public Class<Value> getValueClass() { return Value.class; }
		@Override public Long decodeKey(ByteBuffer bb) { return bb.ReadLong(); }
		@Override public ByteBuffer encodeKey(Long key) {
			var bb = ByteBuffer.Allocate();
			bb.WriteLong(key);
			return bb;
		}
		@Override public Long decodeKeyResultSet(ResultSet rs) throws SQLException { return rs.getLong("__key"); }
		@Override public void encodeKeySQLStatement(SQLStatement st, Long key) { st.appendLong("__key", key); }
		@Override public Value newValue() { return new Value(); }
	}
}
