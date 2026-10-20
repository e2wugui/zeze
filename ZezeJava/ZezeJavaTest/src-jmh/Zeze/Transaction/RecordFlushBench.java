package Zeze.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SQLStatement;
import Zeze.Util.RocksDatabase;
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
import org.rocksdb.PerfContext;
import org.rocksdb.PerfLevel;
import org.rocksdb.RocksDB;

/** 完整刷盘周期：重新置脏、编码、写主库和本地 Rocks 缓存、提交、清脏；A/C 使用同一基准。 */
@NullMarked
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class RecordFlushBench {
	@Param({"1", "1024"})
	public int batchSize;

	@Param({"64", "4096"})
	public int valueSize;

	private Application app;
	private Values table;
	private Checkpoint checkpoint;
	private final List<Record> records = new ArrayList<>();
	private @Nullable RocksDB diagnosticsDb;
	private @Nullable PerfContext diagnosticsPerf;
	private long diagnosticsSequence;
	private long diagnosticsStarted;

	@Setup(Level.Trial)
	public void setup() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(Integer.getInteger("recordFlush.serverId", 10991)); // 默认 JMH forks 串行；诊断可隔离实例。
		conf.setCheckpointMode(CheckpointMode.Table);
		conf.setCheckpointPeriod(Integer.MAX_VALUE);
		conf.setDefaultTableConf(new Config.TableConf());
		var db = new Config.DatabaseConf();
		db.setDatabaseUrl("jmh_record_flush");
		conf.getDatabaseConfMap().put("", db);
		app = new Application("JmhRecordFlush", conf);
		table = new Values();
		app.addTable("", table);
		app.start();
		checkpoint = Objects.requireNonNull(app.getCheckpoint());
		for (long key = 0; key < batchSize; key++) {
			long k = key;
			long rc = app.newProcedure(() -> {
				table.put(k, new Value(new byte[valueSize]));
				return 0;
			}, "RecordFlushBench.prefill").call();
			if (rc != 0)
				throw new IllegalStateException("prefill=" + rc);
			records.add(Objects.requireNonNull(Objects.requireNonNull(table.getCache()).get(k)));
		}
		checkpoint.flush(records, null, null);
		if (Boolean.getBoolean("recordFlush.nativeProfile")) {
			var field = DatabaseRocksDb.class.getDeclaredField("rocksDb");
			field.setAccessible(true);
			var cache = Objects.requireNonNull(app.getLocalRocksCacheDb());
			diagnosticsDb = ((RocksDatabase)Objects.requireNonNull(field.get(cache))).getRocksDb();
		}
	}

	/** 诊断只在迭代边界运行；以本地写入序列号计数，不修改被测方法。 */
	@Setup(Level.Iteration)
	public void startDiagnostics() {
		var db = diagnosticsDb;
		if (db == null)
			return;
		db.setPerfLevel(PerfLevel.ENABLE_TIME_EXCEPT_FOR_MUTEX);
		diagnosticsPerf = db.getPerfContext();
		diagnosticsPerf.reset();
		diagnosticsSequence = db.getLatestSequenceNumber();
		diagnosticsStarted = System.nanoTime();
	}

	@TearDown(Level.Iteration)
	public void reportDiagnostics() {
		var db = diagnosticsDb;
		if (db == null)
			return;
		long elapsed = System.nanoTime() - diagnosticsStarted;
		long writes = db.getLatestSequenceNumber() - diagnosticsSequence;
		if (writes <= 0 || writes % batchSize != 0)
			throw new IllegalStateException("unexpected diagnostic write count=" + writes);
		long operations = writes / batchSize;
		var perf = Objects.requireNonNull(diagnosticsPerf);
		System.out.printf("RECORD_FLUSH_NATIVE ops=%d meanNs=%.3f walNs=%.3f memtableNs=%.3f delayNs=%.3f "
				+ "waitNs=%.3f scheduleNs=%.3f prepostNs=%.3f%n", operations, elapsed / (double)operations,
				perf.getWriteWalTime() / (double)operations, perf.getWriteMemtableTime() / (double)operations,
				perf.getWriteDelayTime() / (double)operations, perf.getWriteThreadWaitNanos() / (double)operations,
				perf.getWriteSchedulingFlushesCompactionsTime() / (double)operations,
				perf.getWritePreAndPostProcessTime() / (double)operations);
	}

	@Benchmark
	public void flush() {
		for (var record : records)
			record.setDirty(true);
		checkpoint.flush(records, null, null);
	}

	@TearDown(Level.Trial)
	public void tearDown() throws Exception {
		try {
			for (long key = 0; key < batchSize; key++) {
				if (Objects.requireNonNull(table.selectFromDatabase(key)).bytes.length != valueSize)
					throw new IllegalStateException("persisted value size");
			}
		} finally {
			app.stop();
		}
	}

	private static final class Value extends Bean {
		private byte[] bytes;

		Value(byte[] bytes) { this.bytes = bytes; }
		@Override public void encode(ByteBuffer bb) { bb.WriteBytes(bytes); }
		@Override public void decode(IByteBuffer bb) { bytes = bb.ReadBytes(); }
		@Override public Value copy() { return new Value(bytes.clone()); }
	}

	private static final class Values extends TableX<Long, Value> {
		Values() { super(991901, "JmhRecordFlush_Values"); }
		@Override public Class<Long> getKeyClass() { return Long.class; }
		@Override public Class<Value> getValueClass() { return Value.class; }
		@Override public Long decodeKey(ByteBuffer bb) { return bb.ReadLong(); }
		@Override public ByteBuffer encodeKey(Long key) {
			var bb = ByteBuffer.Allocate(9);
			bb.WriteLong(key);
			return bb;
		}
		@Override public Value newValue() { return new Value(new byte[0]); }
		@Override public Long decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
			return rs.getLong("__key");
		}
		@Override public void encodeKeySQLStatement(SQLStatement st, Long key) { st.appendLong("__key", key); }
	}
}
