package Zeze.Raft;

import org.jspecify.annotations.Nullable;

import java.io.RandomAccessFile;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Transaction.Procedure;

/**
 * leader 侧一次 InstallSnapshot 发送会话的状态：快照文件句柄、边界信息与逐块发送/应答驱动。
 */
class InstallSnapshotState {
	// Rpc 的 sessionId 不可重用，快照边界由本状态持有，每块新建 InstallSnapshot 发送。
	private @Nullable RaftLog firstLog;
	private @Nullable RandomAccessFile file;
	// 会话所属连接，收口时用于复位索引与续复制。
	private Server.@Nullable ConnectorEx connector;
	// 启动时绑定的 socket，供断线异步回调校验归属。
	private @Nullable AsyncSocket socket;
	private long offset;
	private long term;
	private String leaderId = "";
	private long lastIncludedIndex;
	private long lastIncludedTerm;
	private boolean done;
	private long resultCode = Procedure.ErrorSendFail;

	public @Nullable RaftLog getFirstLog() {
		return firstLog;
	}

	public void setFirstLog(@Nullable RaftLog value) {
		firstLog = value;
	}

	public @Nullable RandomAccessFile getFile() {
		return file;
	}

	public void setFile(RandomAccessFile value) {
		file = value;
	}

	public Server.@Nullable ConnectorEx getConnector() {
		return connector;
	}

	public void setConnector(Server.ConnectorEx value) {
		connector = value;
	}

	public @Nullable AsyncSocket getSocket() {
		return socket;
	}

	public void setSocket(@Nullable AsyncSocket value) {
		socket = value;
	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long value) {
		offset = value;
	}

	public long getTerm() {
		return term;
	}

	public void setTerm(long value) {
		term = value;
	}

	public String getLeaderId() {
		return leaderId;
	}

	public void setLeaderId(String value) {
		leaderId = value;
	}

	public long getLastIncludedIndex() {
		return lastIncludedIndex;
	}

	public void setLastIncludedIndex(long value) {
		lastIncludedIndex = value;
	}

	public long getLastIncludedTerm() {
		return lastIncludedTerm;
	}

	public void setLastIncludedTerm(long value) {
		lastIncludedTerm = value;
	}

	public boolean getDone() {
		return done;
	}

	public long getResultCode() {
		return resultCode;
	}

	public void trySend(LogSequence ls, Server.ConnectorEx c) throws Exception {
		ls.getRaft().lock();
		try {
			if (!ls.getSendSnapshotting().isCurrent(c, this))
				return; // 安装已取消

			if (term != ls.getTerm() || done || ls.getRaft().isShutdown || !ls.getRaft().isLeader()) {
				ls.getSendSnapshotting().end(c, this);
				return; // 安装结束
			}

			c.setAppendLogActiveTime(System.currentTimeMillis());

			var buffer = new byte[32 * 1024];
			// 循环读满缓冲区或遇到 EOF 再判定 done，避免短读导致截断块提前结束安装。
			int rc = 0;
			while (rc < buffer.length) {
				//noinspection DataFlowIssue
				int n = file.read(buffer, rc, buffer.length - rc);
				if (n < 0)
					break; // EOF：rc 为 0 时发送 0 字节收尾块
				rc += n;
			}
			var pending = new InstallSnapshot();
			pending.Argument.setTerm(term);
			pending.Argument.setLeaderId(leaderId);
			pending.Argument.setLastIncludedIndex(lastIncludedIndex);
			pending.Argument.setLastIncludedTerm(lastIncludedTerm);
			pending.Argument.setOffset(offset);
			pending.Argument.setData(new Binary(buffer, rc));
			pending.Argument.setDone(rc < buffer.length);
			offset += rc;
			if (pending.Argument.getDone())
				//noinspection DataFlowIssue
				pending.Argument.setLastIncludedLog(new Binary(firstLog.encode()));

			int timeout = ls.getRaft().getRaftConfig().getAppendEntriesTimeout();
			resultCode = Procedure.ErrorSendFail;
			var sendSocket = socket != null ? socket : c.TryGetReadySocket();
			if (!pending.Send(sendSocket, p -> processResult(ls, c, p), timeout))
				ls.getSendSnapshotting().end(c, this);
		} catch (Throwable e) {
			// 异常时必须收口会话，避免条目残留和文件句柄泄漏导致该 follower 长期卡死。
			LogSequence.logger.error("InstallSnapshotState trySend error. c={}", c.getName(), e);
			ls.getSendSnapshotting().end(c, this);
		} finally {
			ls.getRaft().unlock();
		}
	}

	@SuppressWarnings("SameReturnValue")
	private long processResult(LogSequence ls, Server.ConnectorEx c, Protocol<?> p) throws Exception {
		var r = (InstallSnapshot)p;

		ls.getRaft().lock();
		try {
			if (ls.getRaft().isShutdown)
				return Procedure.Success;
			if (!r.isTimeout() && ls.trySetTerm(r.Result.getTerm()) == LogSequence.SetTermResult.Newer) {
				ls.getRaft().convertStateTo(Raft.RaftState.Follower);
				return Procedure.Success; // 中断安装
			}

			if (!ls.getSendSnapshotting().isCurrent(c, this))
				return Procedure.Success;
			if (r.isTimeout() || term != ls.getTerm() || ls.getRaft().isShutdown || !ls.getRaft().isLeader()) {
				ls.getSendSnapshotting().end(c, this);
				return Procedure.Success;
			}

			if (r.getResultCode() != Procedure.Success && r.getResultCode() != InstallSnapshot.ResultCodeNewOffset) {
				ls.getSendSnapshotting().end(c, this);
				return Procedure.Success; // 中断安装
			}

			resultCode = r.getResultCode();
			done = resultCode == Procedure.Success && r.Argument.getDone();

			// NewOffset 可能应答最后一块，须收到成功应答后才结束发送。
			if (!done && r.Result.getOffset() >= 0) {
				//noinspection DataFlowIssue
				if (r.Result.getOffset() > file.length()) {
					LogSequence.logger.error("InstallSnapshot.Result.Offset Too Big. {}/{}",
							r.Result.getOffset(), file.length());
					ls.getSendSnapshotting().end(c, this);
					return Procedure.Success; // 中断安装
				}
				file.seek(offset = r.Result.getOffset());
			}
			trySend(ls, c);
		} finally {
			ls.getRaft().unlock();
		}
		return Procedure.Success;
	}
}
