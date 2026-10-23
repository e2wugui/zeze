package Benchmark;

import java.util.concurrent.TimeUnit;

import Zeze.Application;
import Zeze.Config;
import Zeze.History.History;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Changes;
import Zeze.Transaction.CheckpointMode;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Transaction.Database;
import Zeze.Transaction.IHistory;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Table;
import demo.ModuleGTable.Bean1;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import Zeze.Util.Id128;

/**
 * Single-threaded transactions over the existing generated ModuleGTable tables.
 * entries is the number of cells in EACH of rows existing rows. Each commit changes row 0, column 0:
 * GTABLE1 replaces its scalar Integer; GTABLE2 edits the existing Bean1.IntVar in place.
 * Compare the same family/size/consumer row across revisions, not the two different value schemas.
 *
 * HISTORY_AND_LISTENER_ENCODE measures two-level source collection, production History.buildLogChanges
 * capture, and a synchronous listener encoding the same Changes.Record again. It does not measure
 * full-value listener projection, network output, replay, external gid allocation or tHistory database IO.
 * The capture sink retains only a number and its PendingHistory is an empty completion token, so long
 * iterations cannot accumulate history payloads. NONE keeps Application.history null and has no listener.
 * Uses only APIs shared by the revisions before and after the Map2 phase split; no version adapter needed.
 */
@NullMarked
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Threads(1)
public class GTableCommitBench {

	@Param({"GTABLE1", "GTABLE2"})
	public String family;

	@Param({"16", "1024"})
	public int entries;

	@Param({"8"})
	public int rows;

	@Param({"NONE", "HISTORY_AND_LISTENER_ENCODE"})
	public String consumers;

	private Application app;
	private demo.ModuleGTable.tGTable scalarTable;
	private demo.ModuleGTable.tGTable2 beanTable;
	private boolean scalarFamily;
	private boolean withConsumers;
	private long sequence;
	private long measuredCommits;
	private long prepares;
	private long captures;
	private long notifications;
	private long historySink;
	private long listenerSink;
	private int expectedValue;
	private @Nullable Throwable listenerFailure;

	@Setup(Level.Trial)
	public void setup() throws Exception {
		if (entries <= 0 || rows <= 0)
			throw new IllegalArgumentException("require entries > 0 and rows > 0");
		scalarFamily = switch (family) {
			case "GTABLE1" -> true;
			case "GTABLE2" -> false;
			default -> throw new IllegalArgumentException(family);
		};
		withConsumers = switch (consumers) {
			case "NONE" -> false;
			case "HISTORY_AND_LISTENER_ENCODE" -> true;
			default -> throw new IllegalArgumentException(consumers);
		};

		var conf = new Config();
		conf.setServerId(Integer.getInteger("gTableBench.serverId", 10993));
		conf.setServiceManager("disable");
		conf.setTakeoverMode("off");
		conf.setCheckpointMode(CheckpointMode.Table);
		conf.setCheckpointPeriod(Integer.MAX_VALUE);
		conf.setDefaultTableConf(new Config.TableConf());
		var db = new Config.DatabaseConf();
		db.setDatabaseUrl("jmh_gtable_commit_" + conf.getServerId());
		conf.getDatabaseConfMap().put("", db);
		app = new Application("JmhGTableCommit", conf);
		app.setSchemas(new demo.Schemas());
		scalarTable = new demo.ModuleGTable.tGTable();
		beanTable = new demo.ModuleGTable.tGTable2();
		Table table = scalarFamily ? scalarTable : beanTable;
		app.addTable("", table);
		if (withConsumers) {
			var field = Application.class.getDeclaredField("history");
			field.setAccessible(true);
			field.set(app, new CaptureHistory(table)); // Test-only assembly; the actual commit hook invokes it.
		}
		try {
			app.start();
			checkResult(app.newProcedure(() -> {
				if (scalarFamily) {
					var values = scalarTable.getOrAdd(1L).getGTable();
					values.clear();
					for (int row = 0; row < rows; ++row)
						for (int column = 0; column < entries; ++column)
							values.put(row, column, 1);
				} else {
					var values = beanTable.getOrAdd(1L).getGTable();
					values.clear();
					for (int row = 0; row < rows; ++row)
						for (int column = 0; column < entries; ++column)
							values.put(row, column, new Bean1(1));
				}
				return 0L;
			}, "GTableCommitBench.prefill").call());
			app.checkpointRun();
			if (withConsumers)
				table.getChangeListenerMap().addListener(this::consumeListener);
			prepares = captures = 0;
			commit(); // Check real consumers before entering timed iterations.
			verifyConsumers();
			measuredCommits = prepares = captures = notifications = 0;
		} catch (Throwable ex) {
			try {
				app.stop();
			} catch (Throwable stopFailure) {
				ex.addSuppressed(stopFailure);
			}
			throw ex;
		}
	}

	private static void checkResult(long result) {
		if (result != 0)
			throw new IllegalStateException("transaction result=" + result);
	}

	@Benchmark
	public long commit() {
		expectedValue = 7 + (int)(++sequence & 1);
		checkResult(app.newProcedure(() -> {
			if (scalarFamily)
				scalarTable.getOrAdd(1L).getGTable().put(0, 0, expectedValue);
			else
				//noinspection DataFlowIssue
				beanTable.getOrAdd(1L).getGTable().get(0, 0).setIntVar(expectedValue);
			return 0L;
		}, "GTableCommitBench.commit").call());
		++measuredCommits;
		if (listenerFailure != null)
			throw new IllegalStateException("listener failed", listenerFailure);
		return historySink ^ listenerSink ^ sequence;
	}

	private void consumeListener(Object key, Changes.Record record) {
		try {
			if (!Long.valueOf(1).equals(key) || record.getState() != Changes.Record.Edit
				|| !(record.getVariableLog(1) instanceof LogMap2<?, ?>))
				throw new IllegalStateException("expected one edited GTable root");
			var source = record.getValue();
			//noinspection DataFlowIssue
			int value = scalarFamily
				? ((demo.ModuleGTable.BValue)source).getGTable().get(0, 0)
				: ((demo.ModuleGTable.BValue2)source).getGTable().get(0, 0).getIntVar();
			if (value != expectedValue)
				throw new IllegalStateException("listener source value=" + value);
			var encoded = ByteBuffer.Allocate();
			record.encode(encoded);
			if (encoded.isEmpty())
				throw new IllegalStateException("empty listener payload");
			listenerSink = encoded.size() + encoded.Bytes[encoded.WriteIndex - 1];
			++notifications;
		} catch (Throwable ex) {
			listenerFailure = ex; // Production isolates callback exceptions; also surface them to the caller.
		}
	}

	private void verifyConsumers() {
		long expected = withConsumers ? measuredCommits : 0;
		if (prepares != expected || captures != expected || notifications != expected)
			throw new IllegalStateException("consumer count: commits=" + measuredCommits + ", prepares=" + prepares
				+ ", captures=" + captures + ", notifications=" + notifications);
	}

	@TearDown(Level.Trial)
	public void tearDown() throws Exception {
		try {
			verifyConsumers();
			checkResult(app.newProcedure(() -> {
				if (scalarFamily) {
					var values = scalarTable.getOrAdd(1L).getGTable();
					if (values.rowKeySet().size() != rows)
						throw new IllegalStateException("final row count=" + values.rowKeySet().size());
					for (int row = 0; row < rows; ++row) {
						if (values.row(row).size() != entries)
							throw new IllegalStateException("final column count=" + values.row(row).size());
						for (int column = 0; column < entries; ++column)
							//noinspection DataFlowIssue
							verifyValue(values.get(row, column), row, column);
					}
				} else {
					var values = beanTable.getOrAdd(1L).getGTable();
					if (values.rowKeySet().size() != rows)
						throw new IllegalStateException("final row count=" + values.rowKeySet().size());
					for (int row = 0; row < rows; ++row) {
						if (values.row(row).size() != entries)
							throw new IllegalStateException("final column count=" + values.row(row).size());
						for (int column = 0; column < entries; ++column)
							//noinspection DataFlowIssue
							verifyValue(values.get(row, column).getIntVar(), row, column);
					}
				}
				return 0L;
			}, "GTableCommitBench.verify").call());
			verifyConsumers(); // A final read-only transaction must not invoke either consumer.
		} finally {
			app.stop();
		}
	}

	private void verifyValue(int value, int row, int column) {
		int expected = row == 0 && column == 0 ? expectedValue : 1;
		if (value != expected)
			throw new IllegalStateException("final value at " + row + ',' + column + '=' + value + ", expected=" + expected);
	}

	private final class CaptureHistory implements IHistory {
		private final Table table;
		private final PendingHistory completed = new PendingHistory() {
			@Override
			public Table getTable() {
				return table;
			}

			@Override
			public void mergeFrom(PendingHistory from) {
				if (from != this)
					throw new IllegalArgumentException("foreign capture-only token");
			}

			@Override
			public PendingHistory snapshot() {
				return this;
			}

			@Override
			public void encode0() {
			}

			@Override
			public void writeOnly(Database.Table target, Database.Transaction transaction) {
			}

			@Override
			public void onPersisted() {
			}
		};

		private CaptureHistory(Table table) {
			this.table = table;
		}

		@Override
		public void registerLogs(Iterable<Table> tables) {
		}

		@Override
		public void afterOpen() {
		}

		@Override
		public void close() {
		}

		@Override
		public PreparedCommit prepareCommit(Procedure procedure) {
			var gid = new Id128(0, ++prepares);
			return changes -> {
				var data = History.buildLogChanges(gid, changes, null, null);
				if (data.getChanges().size() != 1)
					throw new IllegalStateException("expected one encoded record");
				for (var bytes : data.getChanges().values()) {
					if (bytes.size() == 0)
						throw new IllegalStateException("empty history payload");
					historySink = bytes.size() + bytes.bytesUnsafe()[bytes.getOffset() + bytes.size() - 1];
				}
				++captures;
				return completed;
			};
		}
	}
}
