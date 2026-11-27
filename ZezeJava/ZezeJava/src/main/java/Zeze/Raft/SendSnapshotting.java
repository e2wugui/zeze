package Zeze.Raft;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.concurrent.ConcurrentHashMap;

import Zeze.Net.AsyncSocket;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * leader 侧 InstallSnapshot 发送会话登记表：为日志落后到 firstIndex 的 connector 维护分块发送会话。
 * 会话存续期间拦截该 connector 的心跳与日志复制；断线、换主、完成或异常均经 end 收口。
 * 在 LogSequence 构造期初始化，须延迟经 logSequence 访问运行期状态。
 */
public final class SendSnapshotting {
	private static final Logger logger = LogManager.getLogger(SendSnapshotting.class);

	private final LogSequence logSequence;
	private final ConcurrentHashMap<String, InstallSnapshotState> sessions = new ConcurrentHashMap<>();

	public SendSnapshotting(LogSequence logSequence) {
		this.logSequence = logSequence;
	}

	public boolean isEmpty() {
		return sessions.isEmpty();
	}

	// 存在发送会话时拦截该 connector 的心跳与日志复制。
	public boolean contains(String connectorName) {
		return sessions.containsKey(connectorName);
	}

	boolean isCurrent(Server.ConnectorEx c, InstallSnapshotState state) {
		return sessions.get(c.getName()) == state;
	}

	void put(String connectorName, InstallSnapshotState state) {
		sessions.put(connectorName, state);
	}

	public void cancelAll() throws Exception {
		// 关句柄并移除条目，防止换主后残留条目阻塞心跳复制与本地快照。
		for (var e : sessions.entrySet()) {
			if (sessions.remove(e.getKey(), e.getValue()))
				closeSession(e.getValue());
		}
	}

	// 按 connector 移除并收口发送会话。
	public void end(Server.ConnectorEx c) throws Exception {
		closeSession(sessions.remove(c.getName()));
	}

	// 按同一性摘除自身会话，避免误删重连后新建的同名会话（调用者持 Raft 锁）。
	void end(Server.ConnectorEx c, InstallSnapshotState state) throws Exception {
		if (sessions.remove(c.getName(), state))
			closeSession(state);
	}

	// 断线回调按启动时绑定的 socket 校验归属，避免误删重连后的新会话（调用者持 Raft 锁）。
	void endIfSocket(Server.ConnectorEx c, AsyncSocket closed) throws Exception {
		var state = sessions.get(c.getName());
		if (state != null && state.getSocket() == closed && sessions.remove(c.getName(), state))
			closeSession(state);
	}

	// 收口会话：关文件，成功完成时推进索引并继续日志复制（map 移除由调用方负责）。
	private void closeSession(@Nullable InstallSnapshotState state) throws Exception {
		if (state == null)
			return;
		var c = state.getConnector();
		//noinspection DataFlowIssue
		logger.info("{} InstallSnapshot LastIncludedIndex={} Done={} c={}", logSequence.getRaft().getName(),
				state.getLastIncludedIndex(), state.getDone(), c.getName());
		// 容忍打开文件失败留下的 file==null，避免 cancelAll 抛 NPE 中断关闭流程。
		if (state.getFile() != null)
			state.getFile().close();
		if (state.getDone() && state.getResultCode() == 0) {
			c.setNextIndex(state.getLastIncludedIndex() + 1);

			if (state.getLastIncludedIndex() > c.getMatchIndex())
				c.setMatchIndex(state.getLastIncludedIndex());
			logSequence.trySendAppendEntries(c, null);
		}
	}

	// follower 落后到 firstIndex 时启动快照安装（仅由 trySendAppendEntries 在 Raft 锁内调用）。
	void start(Server.ConnectorEx c) throws Exception {
		if (contains(c.getName()))
			return;
		var path = logSequence.getCommittedSnapshotFile();
		// 正在创建本地快照时暂不安装，等待后续重试。
		if ((new File(path)).isFile() && !logSequence.getSnapshotting()) {
			var socket = c.TryGetReadySocket();
			if (socket == null)
				return;
			var state = new InstallSnapshotState();
			state.setConnector(c);
			state.setSocket(socket);
			if (sessions.putIfAbsent(c.getName(), state) != null)
				throw new IllegalStateException("Impossible");

			// 初始化失败时及时收口，防止半初始化条目阻塞心跳与复制。
			try {
				state.setFile(new RandomAccessFile(path, "r"));
				state.setFirstLog(logSequence.readLog(logSequence.getFirstIndex()));
			} catch (Exception e) {
				logger.error("{} startInstallSnapshot: open snapshot fail, cancel install. c={}",
						logSequence.getRaft().getName(), c.getName(), e);
				end(c);
				return;
			}
			if (state.getFirstLog() == null) {
				// 缺失 firstIndex 边界日志时取消本次安装，等待新快照推进后恢复。
				logger.error("{} startInstallSnapshot: no log at firstIndex={}, cancel install. c={}",
						logSequence.getRaft().getName(), logSequence.getFirstIndex(), c.getName());
				end(c);
				return;
			}
			state.setTerm(logSequence.getTerm());
			state.setLeaderId(logSequence.getRaft().getName());
			state.setLastIncludedIndex(state.getFirstLog().getIndex());
			state.setLastIncludedTerm(state.getFirstLog().getTerm());

			logger.info("{} InstallSnapshot Start... Path={} c={}",
					logSequence.getRaft().getName(), path, c.getName());
			state.trySend(logSequence, c);
		} else {
			// 快照文件缺失时触发一次本地快照（内部防重入）。
			TaskSpec.ofAction(logSequence::snapshot).name("Snapshot").run();
		}
	}
}
