package Zeze.Raft;

import java.io.RandomAccessFile;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Transaction.Procedure;

class InstallSnapshotState {
	private final InstallSnapshot pending = new InstallSnapshot(); // 【注意】重用这个Rpc
	private RaftLog firstLog;
	private RandomAccessFile file;
	private long offset;

	public InstallSnapshot getPending() {
		return pending;
	}

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

	public void trySend(LogSequence ls, Server.ConnectorEx c) throws Exception {
		ls.getRaft().lock();
		try {
			if (!ls.getInstallSnapshotting().containsKey(c.getName()))
				return; // 安装取消了。

			if (pending.Argument.getDone() || ls.getRaft().isShutdown || !ls.getRaft().isLeader()) {
				ls.endInstallSnapshot(c);
				return; // install done
			}

			c.setAppendLogActiveTime(System.currentTimeMillis());

			var buffer = new byte[32 * 1024];
			int rc = file.read(buffer);
			pending.Argument.setOffset(offset);
			pending.Argument.setData(new Binary(buffer, rc));
			pending.Argument.setDone(rc < buffer.length);
			offset += rc;
			if (pending.Argument.getDone())
				pending.Argument.setLastIncludedLog(new Binary(firstLog.encode()));

			int timeout = ls.getRaft().getRaftConfig().getAppendEntriesTimeout();
			pending.setResultCode(Procedure.ErrorSendFail);
			if (!pending.Send(c.TryGetReadySocket(), p -> processResult(ls, c, p), timeout))
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
