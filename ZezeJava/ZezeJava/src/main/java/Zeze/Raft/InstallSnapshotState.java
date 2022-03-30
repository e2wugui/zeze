package Zeze.Raft;

import java.io.RandomAccessFile;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Transaction.Procedure;

class InstallSnapshotState {
	private final InstallSnapshot Pending = new InstallSnapshot(); // 【注意】重用这个Rpc
	private RaftLog FirstLog;
	private RandomAccessFile File;
	private long Offset;

	public InstallSnapshot getPending() {
		return Pending;
	}

	public RaftLog getFirstLog() {
		return FirstLog;
	}

	public void setFirstLog(RaftLog value) {
		FirstLog = value;
	}

	public RandomAccessFile getFile() {
		return File;
	}

	public void setFile(RandomAccessFile value) {
		File = value;
	}

	public long getOffset() {
		return Offset;
	}

	public void setOffset(long value) {
		Offset = value;
	}

	public void TrySend(LogSequence ls, Server.ConnectorEx c) throws Throwable {
		synchronized (ls.getRaft()) {
			if (!ls.getInstallSnapshotting().containsKey(c.getName()))
				return; // 安装取消了。

			if (Pending.Argument.getDone() || ls.getRaft().IsShutdown || !ls.getRaft().isLeader()) {
				ls.EndInstallSnapshot(c);
				return; // install done
			}

			c.setAppendLogActiveTime(System.currentTimeMillis());

			var buffer = new byte[32 * 1024];
			int rc = File.read(buffer);
			Pending.Argument.setOffset(Offset);
			Pending.Argument.setData(new Binary(buffer, 0, rc));
			Pending.Argument.setDone(rc < buffer.length);
			Offset += rc;
			if (Pending.Argument.getDone())
				Pending.Argument.setLastIncludedLog(new Binary(FirstLog.Encode()));

			int timeout = ls.getRaft().getRaftConfig().getAppendEntriesTimeout();
			Pending.setResultCode(Procedure.ErrorSendFail);
			if (!Pending.Send(c.TryGetReadySocket(), p -> ProcessResult(ls, c, p), timeout))
				ls.EndInstallSnapshot(c);
		}
	}

	@SuppressWarnings("SameReturnValue")
	private long ProcessResult(LogSequence ls, Server.ConnectorEx c, Protocol p) throws Throwable {
		var r = (InstallSnapshot)p;

		synchronized (ls.getRaft()) {
			if (r.isTimeout()) {
				ls.EndInstallSnapshot(c);
				return Procedure.Success;
			}

			if (ls.TrySetTerm(r.Result.getTerm()) == LogSequence.SetTermResult.Newer) {
				ls.EndInstallSnapshot(c);
				// new term found.
				ls.getRaft().ConvertStateTo(Raft.RaftState.Follower);
				return Procedure.Success; // break install
			}

			if (r.getResultCode() != Procedure.Success && r.getResultCode() != InstallSnapshot.ResultCodeNewOffset) {
				ls.EndInstallSnapshot(c);
				return Procedure.Success; // break install
			}

			if (!r.Argument.getDone() && r.Result.getOffset() >= 0) {
				if (r.Result.getOffset() > File.length()) {
					LogSequence.logger.error("InstallSnapshot.Result.Offset Too Big. {}/{}",
							r.Result.getOffset(), File.length());
					ls.EndInstallSnapshot(c);
					return Procedure.Success; // 中断安装。
				}
				File.seek(Offset = r.Result.getOffset());
			}
			TrySend(ls, c);
		}
		return Procedure.Success;
	}
}
