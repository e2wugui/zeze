package Benchmark;

import java.util.concurrent.TimeUnit;

import Zeze.Application;
import Zeze.Config;
import Zeze.History.History;
import Zeze.Transaction.Changes;
import Zeze.Transaction.CheckpointMode;
import Zeze.Transaction.Database;
import Zeze.Transaction.IHistory;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Table;
import Zeze.Util.Id128;
import demo.Module1.BValue;
import demo.Module1.tflush;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.openjdk.jmh.annotations.*;

/** Complete list/set commits, using identical business APIs on both baseline and candidate revisions. */
@NullMarked
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class CollectionCommitBench {
	@Param({"LIST1", "LIST2", "SET1"}) public String family = "LIST1";
	@Param({"REPLACE", "CLEAR"}) public String operation = "REPLACE";
	@Param({"16", "1024"}) public int entries;
	@Param({"NONE", "HISTORY_CAPTURE", "LISTENER", "BOTH"}) public String consumers = "NONE";

	// JMH calls setup before any benchmark or teardown invocation.
	@SuppressWarnings("NotNullFieldNotInitialized")
	private Application app;
	@SuppressWarnings("NotNullFieldNotInitialized")
	private tflush table;
	private int variableId;
	private int previous;
	private int expected;
	private long sequence;
	private long measuredCommits;
	private long captures;
	private long notifications;
	private long historySink;
	private long listenerSink;
	private boolean withHistory;
	private boolean withListener;
	private @Nullable Throwable listenerFailure;

	private static void check(long result) {
		if (result != Procedure.Success) throw new IllegalStateException("transaction result=" + result);
	}

	private static demo.Bean1 item(int value) {
		var bean = new demo.Bean1();
		bean.setV1(value);
		return bean;
	}

	private void fill(BValue root, int first) {
		switch (family) {
		case "LIST1" -> {
			root.getList30().clear();
			for (int i = 0; i < entries; i++) root.getList30().add(i == 0 ? first : i);
		}
		case "LIST2" -> {
			root.getList9().clear();
			for (int i = 0; i < entries; i++) root.getList9().add(item(i == 0 ? first : i));
		}
		case "SET1" -> {
			root.getSet10().clear();
			for (int i = 0; i < entries; i++) root.getSet10().add(i == 0 ? first : i);
		}
		default -> throw new IllegalArgumentException(family);
		}
	}

	private int value(BValue root) {
		return switch (family) {
			case "LIST1" -> root.getList30().get(0);
			case "LIST2" -> root.getList9().get(0).getV1();
			case "SET1" -> root.getSet10().contains(expected) ? expected : Integer.MIN_VALUE;
			default -> throw new IllegalArgumentException(family);
		};
	}

	@Setup(Level.Trial)
	public void setup() throws Exception {
		if (entries <= 0 || operation.equals("EDIT") && !family.equals("LIST2"))
			throw new IllegalArgumentException("EDIT requires LIST2 and entries must be positive");
		withHistory = consumers.equals("HISTORY_CAPTURE") || consumers.equals("BOTH");
		withListener = consumers.equals("LISTENER") || consumers.equals("BOTH");
		var conf = new Config();
		conf.setServerId(Integer.getInteger("collectionCommitBench.serverId", 10995));
		conf.setServiceManager("disable");
		conf.setTakeoverMode("off");
		conf.setCheckpointMode(CheckpointMode.Table);
		conf.setCheckpointPeriod(Integer.MAX_VALUE);
		conf.setDefaultTableConf(new Config.TableConf());
		var db = new Config.DatabaseConf();
		db.setDatabaseUrl("jmh_collection_commit_" + family + '_' + operation + '_' + entries + '_' + consumers);
		conf.getDatabaseConfMap().put("", db);
		app = new Application("JmhCollectionCommit", conf);
		app.setSchemas(new demo.Schemas());
		table = new tflush();
		app.addTable("", table);
		if (withHistory) {
			var field = Application.class.getDeclaredField("history");
			field.setAccessible(true);
			field.set(app, new CaptureHistory(table));
		}
		try {
			app.start();
			check(app.newProcedure(() -> {
				var root = table.getOrAdd(1L);
				fill(root, 0);
				variableId = switch (family) {
					case "LIST1" -> root.getList30().variableId();
					case "LIST2" -> root.getList9().variableId();
					default -> root.getSet10().variableId();
				};
				return Procedure.Success;
			}, "CollectionCommitBench.prefill").call());
			app.checkpointRun();
			if (withListener) table.getChangeListenerMap().addListener(this::consume);
			captures = 0;
			commit();
			verifyConsumers();
			measuredCommits = captures = notifications = 0;
		} catch (Throwable failure) {
			try {
				app.stop();
			} catch (Throwable stop) {
				failure.addSuppressed(stop);
			}
			throw failure;
		}
	}

	@Benchmark
	public long commit() {
		expected = entries + 7 + (int)(++sequence & 1);
		check(app.newProcedure(() -> {
			var root = table.getOrAdd(1L);
			if (operation.equals("CLEAR")) fill(root, expected);
			else switch (family) {
			case "LIST1" -> root.getList30().set(0, expected);
			case "LIST2" -> {
				if (operation.equals("EDIT")) root.getList9().get(0).setV1(expected);
				else root.getList9().set(0, item(expected));
			}
			case "SET1" -> {
				root.getSet10().remove(previous);
				root.getSet10().add(expected);
			}
			default -> throw new IllegalArgumentException(family);
			}
			return Procedure.Success;
		}, "CollectionCommitBench.commit").call());
		previous = expected;
		++measuredCommits;
		if (listenerFailure != null) throw new IllegalStateException("listener failed", listenerFailure);
		return sequence ^ historySink ^ listenerSink;
	}

	private void consume(Object key, Changes.Record record) {
		try {
			var root = (BValue)record.getValue();
			if (root == null || record.getVariableLog(variableId) == null || value(root) != expected)
				throw new IllegalStateException("incomplete collection notification");
			listenerSink = value(root);
			++notifications;
		} catch (Throwable failure) {
			listenerFailure = failure;
		}
	}

	private void verifyConsumers() {
		if (captures != (withHistory ? measuredCommits : 0) || notifications != (withListener ? measuredCommits : 0))
			throw new IllegalStateException("consumer count mismatch");
	}

	@TearDown(Level.Trial)
	public void tearDown() throws Exception {
		try {
			verifyConsumers();
			check(app.newProcedure(() -> {
				var root = table.get(1L);
				assert root != null;
				int size = switch (family) {
					case "LIST1" -> root.getList30().size();
					case "LIST2" -> root.getList9().size();
					default -> root.getSet10().size();
				};
				if (size != entries || value(root) != expected) throw new IllegalStateException("final state mismatch");
				return Procedure.Success;
			}, "CollectionCommitBench.verify").call());
		} finally {
			app.stop();
		}
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
				return this; // immutable, carries no retained payload
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
			// Encoding only; no replay factories or external ServiceManager required.
		}

		@Override
		public void afterOpen() {
		}

		@Override
		public PreparedCommit prepareCommit(Procedure procedure) {
			return changes -> {
				var data = History.buildLogChanges(new Id128(0, ++captures), changes, null, null);
				if (data.getChanges().size() != 1)
					throw new IllegalStateException("expected one encoded record");
				for (var bytes : data.getChanges().values()) {
					if (bytes.size() == 0)
						throw new IllegalStateException("empty history payload");
					historySink = bytes.size() + bytes.bytesUnsafe()[bytes.getOffset() + bytes.size() - 1];
				}
				return completed;
			};
		}

		@Override
		public void close() {
		}
	}
}
