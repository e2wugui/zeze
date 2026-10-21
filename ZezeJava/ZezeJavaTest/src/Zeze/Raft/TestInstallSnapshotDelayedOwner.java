package Zeze.Raft;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Util.Task;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static java.util.Objects.requireNonNull;

/**
 * 两个真实接收入口共享真实日志库，仅控制旧请求在准入与写块之间的调度。
 * 不启动 server，不绑定端口，不创建 Application；每个测试使用独立临时目录。
 */
@Fast
@NullMarked
public class TestInstallSnapshotDelayedOwner {
	private static final long SNAPSHOT_INDEX = 5;
	private static final String OLD_LEADER = "127.0.0.1:19691";
	private static final String NEW_LEADER = "127.0.0.1:19692";

	private static final class AddLog extends Log {
		static final int TYPE = 0x5a31f072;
		int delta;

		AddLog() {
			super(null);
		}

		AddLog(int delta) {
			this();
			this.delta = delta;
		}

		@Override
		public long typeId() {
			return TYPE;
		}

		@Override
		public void encode(ByteBuffer bb) {
			super.encode(bb);
			bb.WriteInt(delta);
		}

		@Override
		public void decode(IByteBuffer bb) {
			super.decode(bb);
			delta = bb.ReadInt();
		}

		@Override
		public void apply(RaftLog holder, StateMachine stateMachine) {
			((CounterStateMachine)stateMachine).value += delta;
		}
	}

	private static final class CounterStateMachine extends StateMachine {
		int value;
		@Nullable Runnable decodeGate;

		CounterStateMachine() {
			addFactory(AddLog.TYPE, () -> {
				var gate = decodeGate;
				decodeGate = null; // 只在边界 readLog 的第一次解码调度旧请求。
				if (gate != null)
					gate.run();
				return new AddLog();
			});
		}

		@Override
		public SnapshotResult snapshot(String path) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void loadSnapshot(String path) throws Exception {
			value = Integer.parseInt(Files.readString(Path.of(path)));
		}
	}

	/** 完整经过 Rpc 应答编码，记录应答次数；没有真实网络或共享静态状态。 */
	private static final class ReplySocket extends AsyncSocket {
		int replies;

		ReplySocket(Service service) {
			super(service);
		}

		@Override
		public Type getType() {
			return Type.eClient;
		}

		@Override
		public @Nullable SocketAddress getRemoteAddress() {
			return null;
		}

		@Override
		public @Nullable TimeThrottle getTimeThrottle() {
			return null;
		}

		@Override
		public boolean isClosed() {
			return false;
		}

		@Override
		protected void doClose(@Nullable Throwable ex, boolean gracefully) {
		}

		@Override
		public boolean Send(byte[] bytes, int offset, int length) {
			replies++;
			return true;
		}
	}

	private record Observed(@Nullable Throwable installFailure, long first, long last, long committed,
			long applied, int value, boolean prefixPresent, int fatalCalls, boolean replicationAccepted) {
	}

	private static final class Fixture implements AutoCloseable {
		final CounterStateMachine sm = new CounterStateMachine();
		final AtomicInteger fatalCalls = new AtomicInteger();
		final Service replies = new Service("snapshot-delayed-owner");
		final Raft raft;
		final LogSequence seq;

		Fixture(Path home) throws Exception {
			Task.tryInitThreadPool();
			var conf = RaftConfig.loadFromString("""
					<raft Name="127.0.0.1:19690" DbHome="%s">
					    <node Host="127.0.0.1" Port="19690"/>
					    <node Host="127.0.0.1" Port="19691"/>
					    <node Host="127.0.0.1" Port="19692"/>
					</raft>
					""".formatted(home));
			raft = new Raft(sm, "127.0.0.1:19690", conf);
			seq = raft.getLogSequence();
			raft.setFatalKillHookForTest(fatalCalls::incrementAndGet);
			// 不持任何锁地排空真实 timer，然后等待构造时启动的前缀清理。
			var timer = (Future<?>)field(Raft.class, raft, "timerTask");
			if (timer != null)
				timer.cancel(false);
			var cleanup = seq.removeLogBeforeFuture;
			if (cleanup != null)
				cleanup.await();
			raft.lock();
			try {
				// 完整且未提交的旧前缀；索引0的初始边界由真实LogSequence构造。
				for (long index = 1; index <= SNAPSHOT_INDEX; index++)
					seq.saveLog(new RaftLog(0, index,
							index == SNAPSHOT_INDEX ? new AddLog() : new HeartbeatLog()));
				setLongField(seq, "lastIndex", SNAPSHOT_INDEX);
				seq.trySetTerm(1);
				raft.setLeaderId(OLD_LEADER);
			} finally {
				raft.unlock();
			}
		}

		InstallSnapshot snapshot(long term, long index, int value, boolean done) {
			var request = new InstallSnapshot();
			request.setSender(new ReplySocket(replies));
			request.Argument.setTerm(term);
			request.Argument.setLeaderId(term == 1 ? OLD_LEADER : NEW_LEADER);
			request.Argument.setLastIncludedIndex(index);
			request.Argument.setLastIncludedTerm(index == SNAPSHOT_INDEX ? 1 : 2);
			request.Argument.setData(new Binary(Integer.toString(value).getBytes(StandardCharsets.UTF_8)));
			request.Argument.setDone(done);
			request.Argument.setLastIncludedLog(new Binary(new RaftLog(
					request.Argument.getLastIncludedTerm(), index, new HeartbeatLog()).encode()));
			return request;
		}

		Observed race(boolean staleGc) throws Exception {
			var receiveLock = (ReentrantLock)requireNonNull(
					field(ReceiveSnapshotting.class, raft.receiveSnapshotting, "lock"));
			var raftLock = (ReentrantLock)requireNonNull(field(Raft.class, raft, "mutex"));
			var older = snapshot(1, SNAPSHOT_INDEX, 11, false);
			var current = snapshot(2, SNAPSHOT_INDEX, 42, true);
			var olderDone = new CountDownLatch(1);
			var olderFailure = new AtomicReference<@Nullable Throwable>();
			var oldThread = new Thread(() -> {
				try {
					if (staleGc)
						raft.gcReceiveSnapshotting(System.currentTimeMillis());
					else
						raft.processInstallSnapshot(older);
				} catch (Throwable e) {
					olderFailure.set(e);
				} finally {
					olderDone.countDown();
				}
			}, "snapshot-older-owner");
			Throwable failure = null;
			receiveLock.lock();
			try {
				oldThread.start();
				await(() -> receiveLock.hasQueuedThread(oldThread), "旧请求必须到达Receive锁");
				if (raftLock.isLocked()) {
					// 合法的Raft→Receive串行修法：旧请求持Raft锁，不强迫B越过它。
					receiveLock.unlock();
					assertTrue(olderDone.await(5, TimeUnit.SECONDS));
				} else {
					// 旧实现A已过Raft准入且放锁；在B的所有权/尺寸复核之后放行A。
					sm.decodeGate = () -> {
						receiveLock.unlock();
						await(() -> olderDone.getCount() == 0 || raftLock.hasQueuedThread(oldThread),
								"旧请求应完成或等待Raft复核，不能要求它在B持Raft锁时完成");
					};
				}
				try {
					raft.processInstallSnapshot(current);
				} catch (Throwable e) {
					failure = e;
				}
			} finally {
				sm.decodeGate = null;
				if (receiveLock.isHeldByCurrentThread())
					receiveLock.unlock();
				oldThread.join(5_000);
				assertFalse(oldThread.isAlive(), "清理后旧请求线程必须自然退出");
			}
			assertNull(olderFailure.get(), "旧请求应完成或被拒绝，不应因关闭句柄失败");
			if (!staleGc)
				assertEquals(1, ((ReplySocket)requireNonNull(older.getSender())).replies);
			if (failure == null) {
				assertEquals(0, current.getResultCode());
				assertEquals(1, ((ReplySocket)requireNonNull(current.getSender())).replies);
			}
			return observe(failure, false);
		}

		Observed observe(@Nullable Throwable failure, boolean accepted) throws Exception {
			return new Observed(failure, seq.getFirstIndex(), seq.getLastIndex(), seq.getCommitIndex(),
					seq.getLastApplied(), sm.value, seq.readLog(1) != null, fatalCalls.get(), accepted);
		}

		boolean replicate(boolean newEntry) throws Exception {
			var request = new AppendEntries();
			request.setSender(new ReplySocket(replies));
			request.Argument.setTerm(2);
			request.Argument.setLeaderId(NEW_LEADER);
			request.Argument.setPrevLogIndex(newEntry ? SNAPSHOT_INDEX : SNAPSHOT_INDEX + 1);
			request.Argument.setPrevLogTerm(newEntry ? 1 : 2);
			request.Argument.setLeaderCommit(SNAPSHOT_INDEX + 1);
			if (newEntry)
				request.Argument.getEntries().add(new Binary(new RaftLog(2, SNAPSHOT_INDEX + 1, new AddLog(7)).encode()));
			raft.lock();
			try {
				assertEquals(0, seq.followerOnAppendEntries(request)); // 真实Raft入口持有同一把锁。
			} finally {
				raft.unlock();
			}
			assertEquals(1, ((ReplySocket)requireNonNull(request.getSender())).replies);
			return request.Result.getSuccess();
		}

		void reinstall(long index, int value) throws Exception {
			var request = snapshot(2, index, value, true);
			assertEquals(0, raft.processInstallSnapshot(request));
			assertEquals(0, request.getResultCode());
			assertEquals(1, ((ReplySocket)requireNonNull(request.getSender())).replies);
		}

		@Override
		public void close() throws Exception {
			// 测试fatal钩子不会退出JVM；仍走真实shutdown，排空任务并关闭真实日志库。
			raft.isShutdown = false;
			raft.setFatalKillHookForTest(null);
			try {
				raft.shutdown();
			} finally {
				replies.stop();
			}
		}
	}

	@Test
	void olderChunkDoesNotDestroyNewerSnapshot(@TempDir Path home) throws Exception {
		try (var fixture = new Fixture(home)) {
			var observed = fixture.race(false);
			System.out.println("install before explicit retry: " + observed);
			// 恢复控制：尚未收到提交推进时，同一有效快照重发可以恢复；不据此误报不可恢复。
			fixture.reinstall(SNAPSHOT_INDEX, 42);
			assertEquals(42, fixture.sm.value);
			assertEquals(SNAPSHOT_INDEX, fixture.seq.getLastApplied());
			assertTrue(fixture.seq.logsAvailable);
			System.out.println("install retry recovered: " + fixture.observe(null, false));
			assertAll(
					() -> assertNull(observed.installFailure(), "旧块不得删除新安装已冻结的源文件"),
					() -> assertEquals(42, observed.value()),
					() -> assertEquals(SNAPSHOT_INDEX, observed.first()),
					() -> assertEquals(SNAPSHOT_INDEX, observed.applied()),
					() -> assertEquals(SNAPSHOT_INDEX, observed.committed()),
					() -> assertEquals(0, observed.fatalCalls()));
		}
	}

	@Test
	void acceptedReplicationAfterSnapshotMustAdvanceBusinessState(@TempDir Path home) throws Exception {
		try (var fixture = new Fixture(home)) {
			var install = fixture.race(false);
			var accepted = fixture.replicate(true);
			accepted &= fixture.replicate(false);
			accepted &= fixture.replicate(false);
			var observed = fixture.observe(install.installFailure(), accepted);
			System.out.println("after append and repeated heartbeats: " + observed);
			// 提交已到X+1时重发X只会按stale成功忽略，不能修复留下的日志缺口。
			fixture.reinstall(SNAPSHOT_INDEX, 42);
			System.out.println("same-boundary retry after commit: " + fixture.observe(null, accepted));
			// 更高边界的显式新快照仍可恢复，证明不是磁盘故障或测试资源泄漏。
			fixture.reinstall(SNAPSHOT_INDEX + 1, 49);
			assertEquals(49, fixture.sm.value);
			assertEquals(SNAPSHOT_INDEX + 1, fixture.seq.getLastApplied());
			assertAll(
					() -> assertTrue(observed.replicationAccepted(), "合法后续复制与心跳应应答成功"),
					() -> assertEquals(49, observed.value(), "成功复制后业务状态必须含快照和后续增量"),
					() -> assertEquals(SNAPSHOT_INDEX + 1, observed.applied()),
					() -> assertEquals(SNAPSHOT_INDEX + 1, observed.committed()),
					() -> assertEquals(0, observed.fatalCalls()));
		}
	}

	@Test
	void oldGcOwnershipDoesNotDeleteNewerFinalizingSnapshot(@TempDir Path home) throws Exception {
		try (var fixture = new Fixture(home)) {
			var install = fixture.race(true);
			var accepted = fixture.replicate(true);
			var observed = fixture.observe(install.installFailure(), accepted);
			System.out.println("old GC followed by normal append: " + observed);
			fixture.reinstall(SNAPSHOT_INDEX + 1, 49);
			assertEquals(49, fixture.sm.value, "显式较高边界重装仍可恢复");
			assertAll(
					() -> assertNull(install.installFailure(), "GC不可使用过期term删除新收尾源文件"),
					() -> assertTrue(observed.replicationAccepted()),
					() -> assertEquals(49, observed.value()),
					() -> assertEquals(SNAPSHOT_INDEX + 1, observed.applied()),
					() -> assertEquals(0, observed.fatalCalls()));
		}
	}

	private static @Nullable Object field(Class<?> type, Object target, String name) throws Exception {
		var field = type.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}

	private static void setLongField(Object target, String name, long value) throws Exception {
		var field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.setLong(target, value);
	}

	private static void await(BooleanSupplier condition, String message) {
		var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (!condition.getAsBoolean() && System.nanoTime() < deadline)
			LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
		assertTrue(condition.getAsBoolean(), message);
	}
}
