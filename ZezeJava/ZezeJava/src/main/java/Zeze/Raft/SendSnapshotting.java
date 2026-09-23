package Zeze.Raft;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.concurrent.ConcurrentHashMap;

import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * leader侧InstallSnapshot发送会话登记表：每个落后到firstIndex的connector一个会话
 * （打开已提交gen快照、逐块发送，由InstallSnapshotState驱动）。
 * 会话存续期间该connector的心跳与日志复制被拦截（contains判断）；断线、换主、
 * 完成、异常都收口到end：移除会话、关文件、复位connector状态。
 * <p>
 * 构造仅可存引用：本表在LogSequence字段初始化期构造（this逃逸），彼时LogSequence
 * 尚未就绪——日志/快照文件/term一律延迟经logSequence读取，构造器中不得触碰。
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

	// 该connector是否有发送会话（有则拦截其心跳与日志复制——发了也肯定失败，属优化）。
	public boolean contains(String connectorName) {
		return sessions.containsKey(connectorName);
	}

	public void put(String connectorName, InstallSnapshotState state) { // 测试直接合成会话
		sessions.put(connectorName, state);
	}

	public void cancelAll() throws Exception {
		// 关句柄同时删条目：换主后残留条目永久拦截该connector的心跳/复制并禁用本地snapshot。
		for (var it = sessions.entrySet().iterator(); it.hasNext(); ) {
			var e = it.next();
			it.remove();
			end(e.getValue());
		}
	}

	// 按connector收口（心跳/复制/断线路径持有connector）：移除会话并收口。
	public void end(Server.ConnectorEx c) throws Exception {
		end(sessions.remove(c.getName()));
	}

	// 会话收口：关文件；done且成功则复位索引并续复制。map移除由调用方完成。
	private void end(InstallSnapshotState state) throws Exception {
		if (state == null)
			return;
		var c = state.getConnector();
		logger.info("{} InstallSnapshot LastIncludedIndex={} Done={} c={}", logSequence.getRaft().getName(),
				state.getLastIncludedIndex(), state.getDone(), c.getName());
		// 防御：start打开文件失败留下的会话file==null，原样NPE会沿shutdown路径的
		// cancelAll打断后续logSequence.close。
		if (state.getFile() != null)
			state.getFile().close();
		if (state.getDone() && state.getResultCode() == 0) {
			c.setNextIndex(state.getLastIncludedIndex() + 1);

			if (state.getLastIncludedIndex() > c.getMatchIndex()) // see EndReceiveInstallSnapshot 6.
				c.setMatchIndex(state.getLastIncludedIndex());
			// start log copy
			logSequence.trySendAppendEntries(c, null);
		}
	}

	// follower日志落后到firstIndex（无prev-log可复制）时启动安装：发送firstIndex指认的
	// 不可变gen文件，与本地提交/清扫不竞争。仅由trySendAppendEntries在raft锁内调用。
	void start(Server.ConnectorEx c) throws Exception {
		if (contains(c.getName()))
			return;
		var path = logSequence.getCommittedSnapshotFile();
		// 如果 Snapshotting，此时不启动安装。
		// 以后重试 AppendEntries 时会重新尝试 Install.
		if ((new File(path)).isFile() && !logSequence.getSnapshotting()) {
			var state = new InstallSnapshotState();
			state.setConnector(c); // 入表前绑定会话所属连接（end收口据此定位）
			if (sessions.putIfAbsent(c.getName(), state) != null)
				throw new IllegalStateException("Impossible");

			// putIfAbsent之后的初始化（open+readLog）失败时回收半初始化会话：残留会让
			// 心跳/复制被拦截（end已加null防御）。
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
				// firstIndex处没有边界日志（不变式破坏）：继续必然NPE，且异常被吞后connector停在
				// 半初始化状态，心跳被拦截、复制永久楔死。取消本次安装，让本地新snapshot推进
				// firstIndex后恢复。
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
			// 这一般的情况是snapshot文件被删除了。
			// 内部会判断，不会启动多个snapshot。
			TaskSpec.ofAction(logSequence::snapshot).name("Snapshot").run();
		}
	}
}
