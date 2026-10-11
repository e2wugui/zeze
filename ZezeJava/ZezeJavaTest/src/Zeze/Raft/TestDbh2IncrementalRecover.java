package Zeze.Raft;

import java.io.File;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import harness.Extra;
import harness.Fast;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import Zeze.Builtin.Dbh2.BBatch;
import Zeze.Builtin.Dbh2.BSplitPut;
import Zeze.Builtin.Dbh2.CommitBatch;
import Zeze.Builtin.Dbh2.PrepareBatch;
import Zeze.Builtin.Dbh2.SplitPut;
import Zeze.Config;
import Zeze.Dbh2.Dbh2StateMachine;
import Zeze.Dbh2.LogAllocateTid;
import Zeze.Dbh2.LogCommitBatch;
import Zeze.Dbh2.LogPrepareBatch;
import Zeze.Dbh2.LogSplitPut;
import Zeze.Dbh2.LogUndoBatch;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** dbh2 在真实本地 RocksDB 上恢复，只启动 Raft 对象，不监听网络端口。 */
@Fast
@Extra
public class TestDbh2IncrementalRecover {
	private static final int SERVER_ID = 8990;
	private static final String WATERMARK_TABLE = "Zeze.Dbh2.AppliedWatermark";
	private static final String CLIENT_ID = "test.dbh2.incrementalRecover";
	private static final Binary RPC_RESULT = binary("original-committed-result");
	@TempDir
	Path temporaryDirectory;
	private String dbHome;

	@BeforeEach
	public void setUp() {
		Task.tryInitThreadPool();
		// 每个用例独占临时根，其他 Gradle 进程清理 cwd 下 *.raft 时不会触及本库。
		dbHome = temporaryDirectory.resolve("dbh2").toAbsolutePath().toString();
	}

	@Test
	public void testRestartWithoutSnapshotKeepsAppliedPrefixAndReplaysOnlyTail() throws Exception {
		try (var node = new HeadlessNode(dbHome)) {
			node.apply(1, put("applied", "first"));
			node.apply(2, new LogAllocateTid(64));
			node.save(3, put("tail", "last")); // 日志已提交到日志库，状态机批次尚未应用。
			assertEquals(2, node.sequence().getLastApplied());
			assertEquals(64, node.machine.getBucket().getTid());
			assertNull(node.machine.getBucket().get(binary("tail")));
		}

		try (var node = new HeadlessNode(dbHome)) {
			assertEquals(2, node.sequence().getLastApplied(), "首次快照前的重启也应定位到应用水位");
			assertEquals(0, node.machine.snapshotLoads, "有效水位无需装载快照");
			assertEquals(binary("first"), node.machine.getBucket().get(binary("applied")));
			assertEquals(64, node.machine.getBucket().getTid(), "已应用的分号日志不得再次增加上界");
			node.sequence().tryApply(node.sequence().readLog(3), 10);
			assertEquals(1, node.machine.splitApplies, "只应用水位之后尚未完成的日志");
			assertEquals(binary("last"), node.machine.getBucket().get(binary("tail")));
			assertEquals(64, node.machine.getBucket().getTid());
		}
	}

	@Test
	public void testRestartWithSnapshotKeepsStateNewerThanSnapshot() throws Exception {
		try (var node = new HeadlessNode(dbHome)) {
			node.apply(1, put("base", "snapshot"));
			var snapshot = node.machine.snapshot(Path.of(dbHome, "snapshot.base.zip").toString());
			assertTrue(snapshot.success);
			assertEquals(1, snapshot.lastIncludedIndex);
			node.apply(2, new LogAllocateTid(64));
			node.apply(3, put("applied", "newer"));
			node.save(4, put("tail", "last"));
		}

		try (var node = new HeadlessNode(dbHome)) {
			assertEquals(1, node.sequence().getFirstIndex());
			assertEquals(3, node.sequence().getLastApplied(), "保留快照后已落盘的状态机前缀");
			assertEquals(0, node.machine.snapshotLoads, "有效水位应跳过 restore");
			assertEquals(binary("newer"), node.machine.getBucket().get(binary("applied")));
			node.sequence().tryApply(node.sequence().readLog(4), 10);
			assertEquals(1, node.machine.splitApplies);
			assertEquals(binary("last"), node.machine.getBucket().get(binary("tail")));
			assertEquals(64, node.machine.getBucket().getTid());
		}
	}

	@Test
	public void testTidRangeBecomesAvailableOnlyAfterAppliedBatchCommit() throws Exception {
		try (var node = new HeadlessNode(dbHome)) {
			node.machine.probeTidBeforeCommit = true;
			node.apply(1, new LogAllocateTid(64));

			// 探针只收集结果；断言在 apply 外，失败不触发状态机 fatalKill。
			assertEquals(0, node.machine.tidBeforeCommit, "Tid 区间不得在上界和水位落盘前发布给发号线程");
			assertInstanceOf(RaftRetryException.class, node.machine.tidBeforeCommitError,
					"提交前没有区间，headless follower 重新申请号段应被拒绝");
			assertEquals(64, node.machine.getBucket().getTid());
			assertEquals(1, node.machine.readAppliedWatermark().index());
			assertEquals(1, node.machine.getTidAllocator().next(node.machine), "提交成功后才可发出区间首号");
		}
	}

	@Test
	public void testLegacyWithoutWatermarkResetsBeforeReplayingLogs() throws Exception {
		try (var node = new HeadlessNode(dbHome)) {
			node.apply(1, put("applied", "first"));
			deleteWatermark(node);
		}
		try (var node = new HeadlessNode(dbHome)) {
			assertEquals(0, node.sequence().getLastApplied());
			assertNull(node.machine.getBucket().get(binary("applied")), "旧格式无快照库先清库，再从日志恢复");
			node.sequence().tryApply(node.sequence().readLog(1), 10);
			assertEquals(binary("first"), node.machine.getBucket().get(binary("applied")));
			assertEquals(1, node.machine.readAppliedWatermark().index());
		}
	}

	@Test
	public void testLegacyWithoutWatermarkRestoresCommittedSnapshot() throws Exception {
		try (var node = new HeadlessNode(dbHome)) {
			node.apply(1, put("base", "snapshot"));
			deleteWatermark(node); // 旧格式快照本身没有水位。
			assertTrue(node.machine.snapshot(Path.of(dbHome, "snapshot.legacy.zip").toString()).success);
			node.apply(2, put("suffix", "newer"));
			deleteWatermark(node); // 模拟旧版本应用后的存活库。
		}
		try (var node = new HeadlessNode(dbHome)) {
			assertEquals(1, node.machine.snapshotLoads, "无水位必须回退已提交快照");
			assertEquals(1, node.sequence().getLastApplied());
			assertNull(node.machine.readAppliedWatermark());
			assertEquals(binary("snapshot"), node.machine.getBucket().get(binary("base")));
			assertNull(node.machine.getBucket().get(binary("suffix")), "restore 必须丢弃超前内容");
			node.sequence().tryApply(node.sequence().readLog(2), 10);
			assertEquals(1, node.machine.splitApplies);
			assertEquals(binary("newer"), node.machine.getBucket().get(binary("suffix")));
		}
	}

	@Test
	public void testMismatchedTermFallsBackToSnapshot() throws Exception {
		saveSnapshotWithSuffix();
		writeWatermark(2, 9);
		assertSnapshotFallback();
	}

	@Test
	public void testWatermarkBeyondTruncatedLogFallsBackToSnapshot() throws Exception {
		saveSnapshotWithSuffix();
		writeWatermark(3, 1); // 日志最后索引只有 2，水位不得跳过不存在的日志。
		assertSnapshotFallback();
	}

	@Test
	public void testWatermarkBeforeSnapshotBoundaryFallsBackToSnapshot() throws Exception {
		saveSnapshotWithSuffix();
		writeWatermark(0, 0);
		assertSnapshotFallback();
	}

	@Test
	public void testMalformedWatermarkFallsBackToSnapshot() throws Exception {
		saveSnapshotWithSuffix();
		try (var db = new RocksDatabase(Path.of(dbHome, "statemachine").toString())) {
			db.getOrAddTable(WATERMARK_TABLE).put(new byte[]{0}, new byte[]{127});
		}
		assertSnapshotFallback();
	}

	@Test
	public void testIncrementalRecoveryRebuildsLiveTransactionsAndTombstones() throws Exception {
		try (var node = new HeadlessNode(dbHome)) {
			node.apply(1, prepare(101));
			node.apply(2, prepare(202));
			node.apply(3, new LogUndoBatch(202, false));
			assertTrue(node.machine.getTransactions().containsKey(101L));
			assertFalse(node.machine.getTransactions().containsKey(202L));
			assertTrue(tombstones(node.machine).containsKey(202L));
		}
		try (var node = new HeadlessNode(dbHome)) {
			assertEquals(3, node.sequence().getLastApplied());
			assertEquals(0, node.machine.snapshotLoads);
			assertEquals(1, node.machine.getTransactions().size(), "未决事务从存活库重建");
			assertEquals(101, node.machine.getTransactions().get(101L).getBatch().getTid());
			assertTrue(tombstones(node.machine).containsKey(202L), "自主 undo 墓碑仍可处理迟到的终局");
			assertFalse(node.machine.getTransactions().containsKey(202L));
			node.apply(4, new LogUndoBatch(202, true));
			assertFalse(tombstones(node.machine).containsKey(202L));
			assertNull(node.machine.getBucket().getTrans().get(transactionKey(202, false)));
			assertNull(node.machine.getBucket().getTrans().get(transactionKey(202, true)));
		}
	}

	@Test
	public void testCrossNodeSnapshotKeepsAppliedRequestAndReplacesOldTransactions() throws Exception {
		var request = uniquePut("source", "committed", System.currentTimeMillis());
		try (var source = new HeadlessNode(dbHome + "/source", SERVER_ID);
			 var target = new HeadlessNode(dbHome + "/target", SERVER_ID + 1)) {
			source.apply(1, prepare(101));
			source.apply(2, prepare(202));
			source.apply(3, new LogUndoBatch(202, false));
			var uniqueLog = new LogSplitPut(request);
			uniqueLog.setRpcResult(RPC_RESULT);
			var holder = source.save(4, uniqueLog);
			source.sequence().tryApply(source.sequence().readLog(holder.getIndex()), 1); // follower 解码路径
			assertAppliedRequest(source, request);
			assertEquals(4, source.machine.readAppliedWatermark().index());
			assertTrue(source.machine.snapshot(Path.of(dbHome, "snapshot.cross-node.zip").toString()).success);

			target.apply(1, prepare(303));
			target.apply(2, new LogUndoBatch(303, false));
			target.apply(3, prepare(404));
			assertTrue(tombstones(target.machine).containsKey(303L));
			assertTrue(target.machine.getTransactions().containsKey(404L));
			target.machine.loadSnapshot(source.sequence().getCommittedSnapshotFile());

			assertAppliedRequest(target, request);
			assertEquals(binary("committed"), target.machine.getBucket().get(binary("source")));
			assertEquals(4, target.machine.readAppliedWatermark().index());
			assertEquals(1, target.machine.getTransactions().size());
			assertTrue(target.machine.getTransactions().containsKey(101L));
			assertFalse(target.machine.getTransactions().containsKey(404L));
			assertTrue(tombstones(target.machine).containsKey(202L));
			assertFalse(tombstones(target.machine).containsKey(303L), "安装快照必须清除被替换库的旧墓碑");
		}
	}

	@Test
	public void testProcessDeathBeforeBatchCommitKeepsPreviousStateAndReplaysTail() throws Exception {
		var createTime = System.currentTimeMillis();
		runCrashProcess("before-commit", createTime);
		var request = uniquePut("crash-tail", "new", createTime);
		try (var node = new HeadlessNode(dbHome)) {
			assertEquals(2, node.sequence().getLastApplied(), "未提交批次不得推进水位");
			assertEquals(2, node.machine.readAppliedWatermark().index());
			assertEquals(binary("first"), node.machine.getBucket().get(binary("base")));
			assertEquals(64, node.machine.getBucket().getTid());
			assertNull(node.machine.getBucket().get(binary("crash-tail")), "业务数据和终态不能先于水位提交");
			assertNull(node.machine.getUniqueAppliedStub(request));
			node.sequence().tryApply(node.sequence().readLog(3), 10);
			assertEquals(1, node.machine.splitApplies);
			assertEquals(binary("new"), node.machine.getBucket().get(binary("crash-tail")));
			assertEquals(3, node.machine.readAppliedWatermark().index());
			assertAppliedRequest(node, request);
		}
	}

	@Test
	public void testProcessDeathAfterBatchCommitSkipsAlreadyAppliedTailAndKeepsResult() throws Exception {
		var createTime = System.currentTimeMillis();
		runCrashProcess("after-commit", createTime);
		var request = uniquePut("crash-tail", "new", createTime);
		try (var node = new HeadlessNode(dbHome)) {
			assertEquals(3, node.sequence().getLastApplied(), "批次提交后尚未推进内存 lastApplied 也应恢复到水位");
			assertEquals(3, node.machine.readAppliedWatermark().index());
			assertEquals(binary("new"), node.machine.getBucket().get(binary("crash-tail")));
			assertAppliedRequest(node, request);
			node.sequence().tryApply(node.sequence().readLog(3), 10);
			assertEquals(0, node.machine.splitApplies, "已提交日志不重复应用");
			node.apply(4, new LogAllocateTid(64));
			assertEquals(128, node.machine.getBucket().getTid(), "已持久化号段上界只增加下一段");
		}
	}

	@Test
	public void testProcessDeathBeforeTransactionCommitRetainsBlobAndTombstone() throws Exception {
		var createTime = System.currentTimeMillis();
		runCrashProcess("before-commit", createTime, "transaction");
		var request = uniqueCommit(202, createTime);
		try (var node = new HeadlessNode(dbHome)) {
			assertEquals(2, node.sequence().getLastApplied());
			assertEquals(binary("old"), node.machine.getBucket().get(binary("crash-transaction")));
			assertNotNull(node.machine.getBucket().getTrans().get(transactionKey(202, false)), "未提交时事务 blob 必须保留");
			assertNotNull(node.machine.getBucket().getTrans().get(transactionKey(202, true)), "未提交时墓碑 marker 必须保留");
			assertTrue(tombstones(node.machine).containsKey(202L));
			assertNull(node.machine.getUniqueAppliedStub(request));
			node.sequence().tryApply(node.sequence().readLog(3), 10);
			assertEquals(1, node.machine.commitApplies);
			assertEquals(binary("new"), node.machine.getBucket().get(binary("crash-transaction")));
			assertNull(node.machine.getBucket().getTrans().get(transactionKey(202, false)));
			assertNull(node.machine.getBucket().getTrans().get(transactionKey(202, true)));
			assertFalse(tombstones(node.machine).containsKey(202L));
			assertEquals(3, node.machine.readAppliedWatermark().index());
			assertAppliedRequest(node, request);
		}
	}

	@Test
	public void testProcessDeathAfterTransactionCommitKeepsDataDeletionAndResultTogether() throws Exception {
		var createTime = System.currentTimeMillis();
		runCrashProcess("after-commit", createTime, "transaction");
		var request = uniqueCommit(202, createTime);
		try (var node = new HeadlessNode(dbHome)) {
			assertEquals(3, node.sequence().getLastApplied());
			assertEquals(binary("new"), node.machine.getBucket().get(binary("crash-transaction")));
			assertNull(node.machine.getBucket().getTrans().get(transactionKey(202, false)));
			assertNull(node.machine.getBucket().getTrans().get(transactionKey(202, true)));
			assertFalse(tombstones(node.machine).containsKey(202L));
			assertAppliedRequest(node, request);
			node.sequence().tryApply(node.sequence().readLog(3), 10);
			assertEquals(0, node.machine.commitApplies, "已提交事务不再次应用");
		}
	}

	private static void deleteWatermark(HeadlessNode node) throws Exception {
		node.machine.getBucket().getDb().getOrAddTable(WATERMARK_TABLE).delete(new byte[]{0});
	}

	private void writeWatermark(long index, long term) throws Exception {
		var encoded = ByteBuffer.Allocate();
		encoded.WriteLong(index);
		encoded.WriteLong(term);
		try (var db = new RocksDatabase(Path.of(dbHome, "statemachine").toString())) {
			db.getOrAddTable(WATERMARK_TABLE).put(new byte[]{0}, encoded.CopyIf());
		}
	}

	private void saveSnapshotWithSuffix() throws Exception {
		try (var node = new HeadlessNode(dbHome)) {
			node.apply(1, put("base", "snapshot"));
			assertTrue(node.machine.snapshot(Path.of(dbHome, "snapshot.base.zip").toString()).success);
			node.apply(2, put("suffix", "newer"));
		}
	}

	private void assertSnapshotFallback() throws Exception {
		try (var node = new HeadlessNode(dbHome)) {
			assertEquals(1, node.machine.snapshotLoads, "无效水位必须恢复已提交快照");
			assertEquals(1, node.sequence().getLastApplied());
			assertEquals(1, node.machine.readAppliedWatermark().index(), "损坏水位由快照中的有效水位替换");
			assertEquals(binary("snapshot"), node.machine.getBucket().get(binary("base")));
			assertNull(node.machine.getBucket().get(binary("suffix")));
			node.sequence().tryApply(node.sequence().readLog(2), 10);
			assertEquals(1, node.machine.splitApplies);
			assertEquals(binary("newer"), node.machine.getBucket().get(binary("suffix")));
			assertEquals(2, node.machine.readAppliedWatermark().index());
		}
	}

	private static LogPrepareBatch prepare(long tid) {
		var request = new PrepareBatch();
		request.Argument.getBatch().setTid(tid);
		return new LogPrepareBatch(request);
	}

	private static byte[] transactionKey(long tid, boolean tombstone) {
		var key = ByteBuffer.Allocate();
		key.WriteLong(tid);
		if (tombstone)
			key.WriteByte(1);
		return key.CopyIf();
	}

	@SuppressWarnings("unchecked")
	private static Map<Long, ?> tombstones(Dbh2StateMachine machine) throws Exception {
		var field = Dbh2StateMachine.class.getDeclaredField("undonePending");
		field.setAccessible(true);
		return (Map<Long, ?>)field.get(machine);
	}

	private static SplitPut uniquePut(String key, String value, long createTime) {
		var request = new SplitPut();
		request.Argument.setFromTransaction(true);
		request.Argument.getPuts().put(binary(key), binary(value));
		request.getUnique().setClientId(CLIENT_ID);
		request.getUnique().setRequestId(1);
		request.setCreateTime(createTime);
		return request;
	}

	private static CommitBatch uniqueCommit(long tid, long createTime) {
		var request = new CommitBatch();
		request.Argument.setTid(tid);
		request.getUnique().setClientId(CLIENT_ID);
		request.getUnique().setRequestId(1);
		request.setCreateTime(createTime);
		return request;
	}

	private static void assertAppliedRequest(HeadlessNode node, RaftRpc<?, ?> request) throws Exception {
		var state = node.sequence().tryGetRequestState(request);
		assertNotNull(state);
		assertTrue(state.isApplied(), "重发请求必须命中跨重启/快照保留的已应用终态");
		assertEquals(RPC_RESULT, state.getRpcResult(), "重发必须取回第一次提交的应答");
	}

	private void runCrashProcess(String phase, long createTime) throws Exception {
		runCrashProcess(phase, createTime, "split");
	}

	private void runCrashProcess(String phase, long createTime, String operation) throws Exception {
		var home = Path.of(dbHome).toAbsolutePath();
		Files.createDirectories(home);
		var argumentFile = home.resolve("crash.args");
		var output = home.resolve("crash.output");
		// argfile 避免 Windows 命令行长度限制，测试工作进程的全运行时 classpath 原样传给子进程。
		// 编码必须用 native.encoding（非UTF-8）：java launcher 按系统本地代码页解码 @argfile
		//（JEP 400 只改 file.encoding，launcher 的 argfile 解码走 native）——UTF-8 写入时
		// classpath 里非 ASCII 路径条目（本机用户目录下的 gradle 缓存 jar、@TempDir 的 home
		// 参数）全部乱码失效，子进程呈现"Zeze类(纯ASCII路径)能加载、log4j(用户名路径)
		// NoClassDefFoundError"（2026-10-11批30/30确定性红；UTF-8/GBK双盲测实证）。
		Files.writeString(argumentFile, "-ea\n-cp\n" + quoteArgument(childClasspath()) + "\n"
				+ CrashLauncher.class.getName() + "\n" + quoteArgument(home.toString()) + "\n"
				+ phase + "\n" + createTime + "\n" + operation + "\n",
				Charset.forName(System.getProperty("native.encoding", "UTF-8")));
		var javaCommand = ProcessHandle.current().info().command().orElse("java");
		var process = new ProcessBuilder(javaCommand, "@" + argumentFile)
				.redirectErrorStream(true).redirectOutput(output.toFile()).start();
		try {
			assertTrue(process.waitFor(30, TimeUnit.SECONDS), "崩溃子进程必须在限时内结束");
			assertEquals(137, process.exitValue(), "子进程必须到达指定 halt 点：\n"
					+ Files.readString(output, StandardCharsets.UTF_8));
		} finally {
			if (process.isAlive()) {
				process.destroyForcibly();
				process.waitFor(10, TimeUnit.SECONDS);
			}
		}
	}

	private static String childClasspath() throws Exception {
		var paths = new LinkedHashSet<String>();
		for (var loader = TestDbh2IncrementalRecover.class.getClassLoader(); loader != null; loader = loader.getParent()) {
			if (loader instanceof URLClassLoader urls) {
				for (var url : urls.getURLs()) {
					if (url.getProtocol().equals("file"))
						paths.add(Path.of(url.toURI()).toAbsolutePath().toString());
				}
			}
		}
		for (var path : System.getProperty("java.class.path").split(java.util.regex.Pattern.quote(File.pathSeparator)))
			paths.add(Path.of(path).toAbsolutePath().toString());
		return String.join(File.pathSeparator, paths);
	}

	private static String quoteArgument(String argument) {
		return '"' + argument.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
	}

	private static Binary binary(String value) {
		return new Binary(value.getBytes(StandardCharsets.UTF_8));
	}

	private static LogSplitPut put(String key, String value) {
		var request = new SplitPut();
		request.Argument.setFromTransaction(true);
		request.Argument.getPuts().put(binary(key), binary(value));
		return new LogSplitPut(request);
	}

	private static final class CountingStateMachine extends Dbh2StateMachine {
		private int snapshotLoads;
		private int splitApplies;
		private int commitApplies;
		private boolean probeTidBeforeCommit;
		private long tidBeforeCommit;
		private Throwable tidBeforeCommitError;
		private String crashPhase;
		private String crashOperation;

		private CountingStateMachine() {
			super(null);
		}

		@Override
		public void loadSnapshot(String path) throws Exception {
			snapshotLoads++;
			super.loadSnapshot(path);
		}

		@Override
		public void applySplitPut(BSplitPut.Data puts) {
			splitApplies++;
			super.applySplitPut(puts);
			if ("before-commit".equals(crashPhase) && "split".equals(crashOperation))
				Runtime.getRuntime().halt(137);
		}

		@Override
		public void commitBatch(long tid) {
			commitApplies++;
			super.commitBatch(tid);
			if ("before-commit".equals(crashPhase) && "transaction".equals(crashOperation))
				Runtime.getRuntime().halt(137);
		}

		@Override
		public void allocateTid(long range) {
			super.allocateTid(range);
			if (probeTidBeforeCommit) {
				try {
					tidBeforeCommit = getTidAllocator().next(this);
				} catch (Throwable error) {
					tidBeforeCommitError = error;
				}
			}
		}

		@Override
		public void apply(RaftLog holder) throws Exception {
			super.apply(holder);
			if ("after-commit".equals(crashPhase))
				Runtime.getRuntime().halt(137);
		}
	}

	private static final class HeadlessNode implements AutoCloseable {
		private final CountingStateMachine machine = new CountingStateMachine();
		private final Raft raft;

		private HeadlessNode(String dbHome) throws Exception {
			this(dbHome, SERVER_ID);
		}

		private HeadlessNode(String dbHome, int serverId) throws Exception {
			machine.setWriteOptions(RocksDatabase.getSyncWriteOptions());
			raft = new Raft(machine, RaftHeadlessSupport.raftName(serverId),
					RaftHeadlessSupport.newRaftConfig(serverId, dbHome), new Config());
			machine.openBucket();
		}

		private LogSequence sequence() {
			return raft.getLogSequence();
		}

		private RaftLog save(long index, Log log) throws Exception {
			var holder = new RaftLog(1, index, log);
			sequence().saveLog(holder);
			return holder;
		}

		private void apply(long index, Log log) throws Exception {
			sequence().tryApply(save(index, log), 1);
		}

		@Override
		public void close() throws Exception {
			try {
				raft.shutdown();
			} finally {
				machine.close();
			}
		}
	}

	/** 真进程死亡演员：不关闭 RocksDB、不调用 shutdown hook，在应用批次的两侧退出。 */
	public static final class CrashLauncher {
		public static void main(String[] args) throws Exception {
			Task.tryInitThreadPool();
			var operation = args[3];
			HeadlessNode node;
			Log log;
			if (operation.equals("transaction")) {
				try (var initial = new HeadlessNode(args[0])) {
					initial.apply(1, put("crash-transaction", "old"));
					initial.apply(2, new LogAllocateTid(64));
					// 有数据的自主 undo 墓碑磁盘形态；无锁装载允许 headless 复活路径使用真实数据。
					var batch = new BBatch.Data();
					batch.setTid(202);
					batch.getPuts().put(binary("crash-transaction"), binary("new"));
					initial.machine.getBucket().getTrans().put(transactionKey(202, false), ByteBuffer.encode(batch).CopyIf());
					initial.machine.getBucket().getTrans().put(transactionKey(202, true), ByteBuffer.Empty);
				}
				node = new HeadlessNode(args[0]);
				log = new LogCommitBatch(uniqueCommit(202, Long.parseLong(args[2])));
			} else {
				node = new HeadlessNode(args[0]);
				node.apply(1, put("base", "first"));
				node.apply(2, new LogAllocateTid(64));
				log = new LogSplitPut(uniquePut("crash-tail", "new", Long.parseLong(args[2])));
			}
			log.setRpcResult(RPC_RESULT);
			node.machine.crashPhase = args[1];
			node.machine.crashOperation = operation;
			node.apply(3, log);
			throw new AssertionError("crash point was not reached");
		}
	}
}
