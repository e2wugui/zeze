package Benchmark;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.BiConsumer;
import Zeze.Application;
import Zeze.Config;
import Zeze.History.History;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Bean;
import Zeze.Transaction.CheckpointMode;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Transaction.Collections.LogSortedMap2;
import Zeze.Transaction.Database;
import Zeze.Transaction.IHistory;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Table;
import Zeze.Util.Id128;
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

/**
 * Full single-threaded transactions over existing generated beans and tables.
 * MAP2 edits BRole.Bag[1001].Count; SORTED_MAP2 edits BValue.sortedmap2[1001].int_1.
 * Compare each parameter row with the same row in the other revision, not the two schemas with each other.
 *
 * HISTORY_CAPTURE installs a capture-only IHistory consumer: the actual commit path collects Changes and
 * History.buildLogChanges encodes the production payload before listener notification. A local gid replaces
 * ServiceManager allocation; the bounded sink does not persist history or measure replay/ledger/database IO.
 * Its PendingHistory is only an empty completion token, so a long iteration cannot accumulate history bytes.
 * NONE leaves Application.history null. LISTENER consumes upserts through the version's production path.
 * Setup resolves a MethodHandle once: newer revisions use Changes.Record.forEach[Sorted]Map2Upsert;
 * older revisions use their mergeChangedToReplaced plus getReplaced. Both callback adapters have the same
 * Record/int/BiConsumer invocation shape, and neither performs per-element reflection.
 * No production phase implementation is copied into the benchmark.
 */
@NullMarked
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Threads(1)
public class MapBeanCommitBench {

	@Param({"MAP2", "SORTED_MAP2"})
	public String family;

	@Param({"EDIT", "REPLACE"})
	public String operation;

	@Param({"16", "1024"})
	public int entries;

	@Param({"NONE", "HISTORY_CAPTURE", "LISTENER", "BOTH"})
	public String consumers;

	// For a larger delta use -p changeCount=64 -p entries=1024; no repeated edits to the same key.
	@Param({"1"})
	public int changeCount;

	private Application app;
	private demo.Game.tRole roles;
	private demo.Module1.Table1 sorted;
	private boolean mapFamily;
	private boolean replace;
	private boolean withHistory;
	private boolean withListener;
	private long sequence;
	private long measuredCommits;
	private long captures;
	private long notifications;
	private long historySink;
	private long listenerSink;
	private int expectedValue;
	private @Nullable Throwable listenerFailure;
	private MethodHandle listenerUpserts;
	private int listenerVariableId;
	private int listenerDeltaCount;
	private long listenerDeltaSum;
	private final BiConsumer<Integer, Bean> consumeUpsert = (key, item) -> {
		++listenerDeltaCount;
		listenerDeltaSum += mapFamily ? ((demo.Game.BItem)item).getCount()
				: ((demo.Module1.BSimple)item).getInt_1();
	};

	@Setup(Level.Trial)
	public void setup() throws Exception {
		if (changeCount <= 0 || changeCount > entries)
			throw new IllegalArgumentException("require 0 < changeCount <= entries");
		mapFamily = switch (family) {
			case "MAP2" -> true;
			case "SORTED_MAP2" -> false;
			default -> throw new IllegalArgumentException(family);
		};
		replace = switch (operation) {
			case "EDIT" -> false;
			case "REPLACE" -> true;
			default -> throw new IllegalArgumentException(operation);
		};
		withHistory = consumers.equals("HISTORY_CAPTURE") || consumers.equals("BOTH");
		withListener = consumers.equals("LISTENER") || consumers.equals("BOTH");
		if (!withHistory && !withListener && !consumers.equals("NONE"))
			throw new IllegalArgumentException(consumers);
		if (withListener) {
			listenerUpserts = resolveListenerUpserts(mapFamily);
			listenerVariableId = mapFamily ? demo.Game.tRole.VAR_Bag : demo.Module1.Table1.VAR_sortedmap2;
		}

		var conf = new Config();
		conf.setServerId(Integer.getInteger("mapBeanBench.serverId", 10992));
		conf.setServiceManager("disable");
		conf.setTakeoverMode("off");
		conf.setCheckpointMode(CheckpointMode.Table);
		conf.setCheckpointPeriod(Integer.MAX_VALUE); // no periodic flush noise in this commit-only benchmark
		conf.setDefaultTableConf(new Config.TableConf());
		var db = new Config.DatabaseConf();
		db.setDatabaseUrl("jmh_map_bean_commit_" + conf.getServerId());
		conf.getDatabaseConfMap().put("", db);
		app = new Application("JmhMapBeanCommit", conf);
		app.setSchemas(new demo.Schemas()); // Table1 requires its generated relational-mapping metadata.
		roles = new demo.Game.tRole();
		sorted = new demo.Module1.Table1();
		Table table = mapFamily ? roles : sorted;
		app.addTable("", table);
		if (withHistory) {
			// Test-only assembly, also used by the existing commit-path tests; no production hook needed.
			var field = Application.class.getDeclaredField("history");
			field.setAccessible(true);
			field.set(app, new CaptureHistory(table));
		}
		try {
			app.start();
			checkResult(app.newProcedure(() -> {
				if (mapFamily) {
					var bag = roles.getOrAdd(1L).getBag();
					for (int i = 0; i < entries; ++i)
						bag.put(1001 + i, newItem(1));
				} else {
					var bag = sorted.getOrAdd(1L).getSortedmap2();
					for (int i = 0; i < entries; ++i)
						bag.put(1001 + i, newSimple(1));
				}
				return 0L;
			}, "MapBeanCommitBench.prefill").call());
			app.checkpointRun();
			if (withListener)
				table.getChangeListenerMap().addListener(this::consumeListener);
			captures = 0;
			// Validate the consumers on a real edit/replace before entering timed iterations.
			commit();
			verifyConsumers();
			measuredCommits = captures = notifications = 0;
		} catch (Throwable ex) {
			try {
				app.stop();
			} catch (Throwable stopFailure) {
				ex.addSuppressed(stopFailure);
			}
			throw ex;
		}
	}

	private static demo.Game.BItem newItem(int value) {
		var item = new demo.Game.BItem();
		item.setCount(value);
		return item;
	}

	private static demo.Module1.BSimple newSimple(int value) {
		var item = new demo.Module1.BSimple();
		item.setInt_1(value);
		return item;
	}

	private static void checkResult(long result) {
		if (result != 0)
			throw new IllegalStateException("transaction result=" + result);
	}

	@Benchmark
	public long commit() {
		expectedValue = 7 + (int)(++sequence & 1);
		checkResult(app.newProcedure(() -> {
			if (mapFamily) {
				var bag = roles.getOrAdd(1L).getBag();
				for (int i = 0; i < changeCount; ++i) {
					if (replace)
						bag.put(1001 + i, newItem(expectedValue));
					else
						Objects.requireNonNull(bag.get(1001 + i)).setCount(expectedValue);
				}
			} else {
				var bag = sorted.getOrAdd(1L).getSortedmap2();
				for (int i = 0; i < changeCount; ++i) {
					if (replace)
						bag.put(1001 + i, newSimple(expectedValue));
					else
						Objects.requireNonNull(bag.get(1001 + i)).setInt_1(expectedValue);
				}
			}
			return 0L; // nonzero would trigger per-operation error logging
		}, "MapBeanCommitBench.commit").call());
		++measuredCommits;
		if (listenerFailure != null)
			throw new IllegalStateException("listener failed", listenerFailure);
		return historySink ^ listenerSink ^ sequence;
	}

	private void consumeListener(Object key, Changes.Record record) {
		try {
			listenerDeltaCount = 0;
			listenerDeltaSum = 0;
			listenerUpserts.invokeExact(record, listenerVariableId, consumeUpsert);
			if (listenerDeltaCount != changeCount || listenerDeltaSum != (long)changeCount * expectedValue)
				throw new IllegalStateException("listener delta: count=" + listenerDeltaCount
						+ ", sum=" + listenerDeltaSum);
			listenerSink = listenerDeltaSum + (Long)key;
			++notifications;
		} catch (Throwable ex) {
			// Production notification catches exceptions; surface failures to the benchmark caller as well.
			listenerFailure = ex;
		}
	}

	private static MethodHandle resolveListenerUpserts(boolean mapFamily) throws ReflectiveOperationException {
		var lookup = MethodHandles.lookup();
		try {
			return lookup.findVirtual(Changes.Record.class,
					mapFamily ? "forEachMap2Upsert" : "forEachSortedMap2Upsert",
					MethodType.methodType(void.class, int.class, BiConsumer.class));
		} catch (NoSuchMethodException beforePhaseSplit) {
			var merge = lookup.findVirtual(mapFamily ? LogMap2.class : LogSortedMap2.class,
					"mergeChangedToReplaced", MethodType.methodType(void.class));
			return lookup.findStatic(MapBeanCommitBench.class,
					mapFamily ? "oldMapUpserts" : "oldSortedMapUpserts",
					MethodType.methodType(void.class, MethodHandle.class, Changes.Record.class,
							int.class, BiConsumer.class)).bindTo(merge);
		}
	}

	@SuppressWarnings("unchecked")
	private static void oldMapUpserts(MethodHandle merge, Changes.Record record, int variableId,
			BiConsumer<Integer, Bean> consumer) throws Throwable {
		var log = (LogMap2<Integer, Bean>)Objects.requireNonNull(record.getVariableLog(variableId));
		merge.invokeExact(log);
		log.getReplaced().forEach(consumer);
	}

	@SuppressWarnings("unchecked")
	private static void oldSortedMapUpserts(MethodHandle merge, Changes.Record record, int variableId,
			BiConsumer<Integer, Bean> consumer) throws Throwable {
		var log = (LogSortedMap2<Integer, Bean>)Objects.requireNonNull(record.getVariableLog(variableId));
		merge.invokeExact(log);
		log.getReplaced().forEach(consumer);
	}

	private void verifyConsumers() {
		if (captures != (withHistory ? measuredCommits : 0)
				|| notifications != (withListener ? measuredCommits : 0))
			throw new IllegalStateException("consumer count: commits=" + measuredCommits
					+ ", captures=" + captures + ", notifications=" + notifications);
	}

	@TearDown(Level.Trial)
	public void tearDown() throws Exception {
		try {
			verifyConsumers();
			checkResult(app.newProcedure(() -> {
				for (int i = 0; i < changeCount; ++i) {
					int value = mapFamily
							? Objects.requireNonNull(roles.getOrAdd(1L).getBag().get(1001 + i)).getCount()
							: Objects.requireNonNull(sorted.getOrAdd(1L).getSortedmap2().get(1001 + i)).getInt_1();
					if (value != expectedValue)
						throw new IllegalStateException("final value=" + value);
				}
				return 0L;
			}, "MapBeanCommitBench.verify").call());
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
