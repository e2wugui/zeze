package Zeze.Raft;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND8-36回归：InstallSnapshotState 跨块重用一次性 Rpc。
 * 旧实现持有 final InstallSnapshot pending 跨块重发：快照.dat ≥ 32768 字节时
 * 第 2 块 pending.Send 因 sessionId!=0 抛 IllegalStateException（Rpc一次性契约，
 * commit 71026dfd6），异常被上层任务吞掉后无人调用 endInstallSnapshot——
 * installSnapshotting 条目残留、文件不关、该 follower 的心跳与复制被永久拦截；
 * file.read/seek 的 IOException 走同一条吞没路径，造成同型楔死。
 * 修复：每块 new 一个 InstallSnapshot（边界信息由 state 字段携带），
 * trySend 外层 catch(Throwable) 收口 endInstallSnapshot。
 * <p>
 * 复现：不起网络。FakeSocket 走通 Send 全路径（不实际发网络），反射把 socket
 * 注入 ConnectorEx、把 Raft 置为 Leader，手工搭好安装状态后直接驱动
 * trySend/processResult（processResult 私有，按 TestFnd757 先例反射调用）。
 * 旧代码在块 2 处抛 IllegalStateException、IO 异常直接传播且条目残留，均失败。
 */
@Fast
public class TestFnd836InstallSnapshotPerChunkRpc {
	private static final String raftName = "127.0.0.1:26360";
	private static final String dbHome = "a3_TestFnd836InstallSnapshot.raft";
	private static final String snapFile = "a3_snapshot.dat";

	// 计数型假socket：接受所有发送但不实际发网络（TestRpcNoReuse.FakeSocket 范式）。
	private static final class CountingSocket extends AsyncSocket {
		int sendCount;

		CountingSocket(Service service) {
			super(service);
		}

		@Override
		public Type getType() {
			return Type.eClient;
		}

		@Override
		public @Nullable java.net.SocketAddress getRemoteAddress() {
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
		public boolean Send(byte @NotNull [] bytes, int offset, int length) {
			sendCount++;
			return true;
		}
	}

	private Raft raft;
	private LogSequence ls;
	private Server.ConnectorEx c;
	private InstallSnapshotState st;
	private CountingSocket socket;

	// 显式DbHome；3节点仅是Raft构造的配置要求，本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:26360" DbHome="a3_TestFnd836InstallSnapshot.raft">
					<node Host="127.0.0.1" Port="26360"/>
					<node Host="127.0.0.1" Port="26361"/>
					<node Host="127.0.0.1" Port="26362"/>
				</raft>
				""");
	}

	@BeforeEach
	public void setUp() {
		Task.tryInitThreadPool();
		LogSequence.deletedDirectoryAndCheck(new java.io.File(dbHome), 100);
	}

	@AfterEach
	public void tearDown() {
		if (raft != null) {
			try {
				if (st != null && st.getFile() != null && st.getFile().getFD().valid())
					st.getFile().close();
			} catch (Exception ignore) {
			}
			try {
				raft.getLogSequence().close();
				raft.shutdown();
			} catch (Exception ignore) {
			}
		}
		LogSequence.deleteDirectory(new java.io.File(dbHome)); // best-effort
	}

	// 手工搭好安装状态（等价 startInstallSnapshot 的 setup，但不依赖 RocksDB 里的
	// firstIndex 边界日志）：快照文件按参数字节数生成。
	private void setupInstall(int snapshotSize) throws Exception {
		var sm = new StateMachine() {
			@Override
			public SnapshotResult snapshot(String path) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void loadSnapshot(String path) {
			}
		};
		raft = new Raft(sm, raftName, newRaftConfig());
		ls = raft.getLogSequence();
		raft.getRaftConfig().setAppendEntriesTimeout(30_000); // 压测下不与超时定时器赛跑

		// 反射置为 Leader：trySend 对非 leader 直接 endInstall。
		var stateField = Raft.class.getDeclaredField("state");
		stateField.setAccessible(true);
		stateField.set(raft, Raft.RaftState.Leader);

		// ConnectorEx 不接真实网络，注入假socket（Rpc.Send 全路径需要非 null socket）。
		socket = new CountingSocket(new Service("a3Fnd836Install"));
		c = new Server.ConnectorEx("127.0.0.1", 26361);
		var futureField = Connector.class.getDeclaredField("futureSocket");
		futureField.setAccessible(true);
		var future = futureField.get(c);
		future.getClass().getMethod("setResult", Object.class).invoke(future, socket);

		var bytes = new byte[snapshotSize];
		Arrays.fill(bytes, (byte)'Z');
		Files.write(Paths.get(dbHome, snapFile), bytes);

		st = new InstallSnapshotState();
		c.setInstallSnapshotState(st);
		st.setFile(new java.io.RandomAccessFile(Paths.get(dbHome, snapFile).toFile(), "r"));
		st.setFirstLog(new RaftLog(1, 6, new HeartbeatLog()));
		st.setTerm(ls.getTerm());
		st.setLeaderId(raft.getName());
		st.setLastIncludedIndex(st.getFirstLog().getIndex());
		st.setLastIncludedTerm(st.getFirstLog().getTerm());
		ls.getInstallSnapshotting().put(c.getName(), c);
	}

	// 伪造已发送块的应答（真实流中应答回到发送实例本身，这里等价构造）。
	private static InstallSnapshot newAck(LogSequence ls, boolean done) {
		var r = new InstallSnapshot();
		r.Argument.setDone(done);
		r.Result.setTerm(ls.getTerm());
		r.Result.setOffset(-1); // 默认让Leader继续顺序传输
		r.setResultCode(Procedure.Success);
		return r;
	}

	// processResult 是私有方法，按 TestFnd757 的先例反射调用。
	private static void processResult(InstallSnapshotState st, LogSequence ls, Server.ConnectorEx c,
									  InstallSnapshot r) throws Exception {
		Method method = InstallSnapshotState.class.getDeclaredMethod("processResult",
				LogSequence.class, Server.ConnectorEx.class, Protocol.class);
		method.setAccessible(true);
		try {
			method.invoke(st, ls, c, r);
		} catch (InvocationTargetException e) {
			throw Task.forceThrow(e.getCause());
		}
	}

	// 快照 40KB：块1(32768,done=false) + 块2(8192,done=true)。
	// 旧代码：ack1 → trySend 块2 → pending.Send 重入抛 IllegalStateException。
	@Test
	public void testMultiChunkInstallCompletes() throws Exception {
		setupInstall(40 * 1024);

		st.trySend(ls, c); // 块1
		assertEquals(1, socket.sendCount, "块1已发出");
		assertEquals(32768, st.getOffset(), "块1推进offset");
		assertTrue(ls.getInstallSnapshotting().containsKey(c.getName()), "安装进行中");

		// 旧代码在此抛 IllegalStateException（一次性Rpc重用），修复后正常发出块2。
		processResult(st, ls, c, newAck(ls, false));
		assertEquals(2, socket.sendCount, "块2必须用新实例发出");
		assertEquals(40 * 1024, st.getOffset(), "块2推进offset");
		assertTrue(ls.getInstallSnapshotting().containsKey(c.getName()), "末块未应答前安装不结束");

		// 末块(done)应答：endInstall 收尾。
		processResult(st, ls, c, newAck(ls, true));
		assertEquals(2, socket.sendCount, "done后不再发块");
		assertTrue(ls.getInstallSnapshotting().isEmpty(), "endInstall必须清理installSnapshotting");
		assertNull(c.getInstallSnapshotState(), "state必须清理");
		assertFalse(st.getFile().getFD().valid(), "快照文件必须关闭");
		assertEquals(7, c.getNextIndex(), "nextIndex推进到lastIncludedIndex+1");
		assertEquals(6, c.getMatchIndex(), "matchIndex推进到lastIncludedIndex");
	}

	// 快照恰 32768 字节（楔死边界，原审计修正点）：块1读满 done=false，
	// 块2是 EOF 0 字节收尾块——旧代码同样在块2楔死。
	@Test
	public void testExactChunkBoundaryInstallCompletes() throws Exception {
		setupInstall(32 * 1024);

		st.trySend(ls, c); // 块1：rc==buffer.length → done=false
		assertEquals(1, socket.sendCount);
		assertFalse(st.getDone(), "读满一块不是done");

		processResult(st, ls, c, newAck(ls, false)); // 块2：EOF 0字节收尾块
		assertEquals(2, socket.sendCount, "边界情形块2（0字节done块）必须能发出");
		assertFalse(st.getDone(), "done在收尾块被应答后才置位");
		assertEquals(32 * 1024, st.getOffset(), "0字节收尾块不推进offset");

		processResult(st, ls, c, newAck(ls, true));
		assertTrue(ls.getInstallSnapshotting().isEmpty(), "安装正常完成");
		assertEquals(7, c.getNextIndex());
	}

	// 孪生：file.read/seek 的 IOException（此处用提前close制造"Stream Closed"）
	// 不得传播且必须收口 endInstall——旧代码异常被上层吞掉后条目残留，follower永久楔死。
	@Test
	public void testIoExceptionEndsInstall() throws Exception {
		setupInstall(40 * 1024);

		st.trySend(ls, c); // 块1
		assertEquals(1, socket.sendCount);
		st.getFile().close(); // 块2的 file.read 将抛 IOException

		processResult(st, ls, c, newAck(ls, false)); // 不得抛出
		assertEquals(1, socket.sendCount, "异常块未发出");
		assertTrue(ls.getInstallSnapshotting().isEmpty(), "异常路径必须清理installSnapshotting");
		assertNull(c.getInstallSnapshotState(), "异常路径必须清理state");
	}
}
