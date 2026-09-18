package Zeze.Raft;

import java.io.RandomAccessFile;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Transaction.Procedure;

class InstallSnapshotState {
	// Rpc实例一次性（sessionId不可重用），跨块重用会在第二次Send时抛IllegalStateException，
	// 之后所有块都发不出去：快照边界信息由state携带，每块new一个InstallSnapshot发送。
	private RaftLog firstLog;
	private RandomAccessFile file;
	private long offset;
	private long term;
	private String leaderId = "";
	private long lastIncludedIndex;
	private long lastIncludedTerm;
	private boolean done;
	private long resultCode = Procedure.ErrorSendFail;

	public RaftLog getFirstLog() {
		return firstLog;
	}

	public void setFirstLog(RaftLog value) {
		firstLog = value;
	}

	public RandomAccessFile getFile() {
		return file;
	}

	public void setFile(RandomAccessFile value) {
		file = value;
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
			if (!ls.getInstallSnapshotting().containsKey(c.getName()))
				return; // 安装取消了。

			if (done || ls.getRaft().isShutdown || !ls.getRaft().isLeader()) {
				ls.endInstallSnapshot(c);
				return; // install done
			}

			c.setAppendLogActiveTime(System.currentTimeMillis());

			var buffer = new byte[32 * 1024];
			int rc = file.read(buffer);
			if (rc < 0)
				rc = 0; // EOF：发0字节收尾块（done=true），完成协议
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
				pending.Argument.setLastIncludedLog(new Binary(firstLog.encode()));

			int timeout = ls.getRaft().getRaftConfig().getAppendEntriesTimeout();
			resultCode = Procedure.ErrorSendFail;
			if (!pending.Send(c.TryGetReadySocket(), p -> processResult(ls, c, p), timeout))
				ls.endInstallSnapshot(c);
		} catch (Throwable e) {
			// 异常会被上层任务记日志后吞掉，这里不收口的话该follower的安装将永久楔死
			// （installSnapshotting条目残留、文件不关、心跳与复制被拦截）。
			LogSequence.logger.error("InstallSnapshotState trySend error. c={}", c.getName(), e);
			ls.endInstallSnapshot(c);
		} finally {
			ls.getRaft().unlock();
		}
	}

	@SuppressWarnings("SameReturnValue")
	private long processResult(LogSequence ls, Server.ConnectorEx c, Protocol<?> p) throws Exception {
		var r = (InstallSnapshot)p;

		ls.getRaft().lock();
		try {
			if (r.isTimeout()) {
				ls.endInstallSnapshot(c);
				return Procedure.Success;
			}

			if (ls.trySetTerm(r.Result.getTerm()) == LogSequence.SetTermResult.Newer) {
				ls.endInstallSnapshot(c);
				// new term found.
				ls.getRaft().convertStateTo(Raft.RaftState.Follower);
				return Procedure.Success; // break install
			}

			if (r.getResultCode() != Procedure.Success && r.getResultCode() != InstallSnapshot.ResultCodeNewOffset) {
				ls.endInstallSnapshot(c);
				return Procedure.Success; // break install
			}

			done = r.Argument.getDone();
			resultCode = r.getResultCode();

			if (!r.Argument.getDone() && r.Result.getOffset() >= 0) {
				if (r.Result.getOffset() > file.length()) {
					LogSequence.logger.error("InstallSnapshot.Result.Offset Too Big. {}/{}",
							r.Result.getOffset(), file.length());
					ls.endInstallSnapshot(c);
					return Procedure.Success; // 中断安装。
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
