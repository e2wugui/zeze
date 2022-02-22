using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Net;
using Zeze.Transaction;

namespace Zeze.Raft
{
    public class InstallSnapshotState
    {
        public InstallSnapshotArgument ReuseArgument { get; set; }
        public RaftLog FirstLog { get; set; }
        public FileStream File { get; set; }
        public long Offset { get; set; }

        public void TrySend(LogSequence ls, Server.ConnectorEx c)
        {
            lock (ls.Raft)
            {
                if (false == ls.InstallSnapshotting.TryGetValue(c.Name, out _))
                {
                    return; // 安装取消了。
                }

                if (ReuseArgument.Done || ls.Raft.IsShutdown || false == ls.Raft.IsLeader)
                {
                    ls.EndInstallSnapshot(c);
                    return; // install done
                }

                c.AppendLogActiveTime = Util.Time.NowUnixMillis;

                var buffer = new byte[32 * 1024];
                int rc = File.Read(buffer);
                ReuseArgument.Offset = Offset;
                ReuseArgument.Data = new Binary(buffer, 0, rc);
                ReuseArgument.Done = rc < buffer.Length;
                Offset += rc;
                if (ReuseArgument.Done)
                {
                    ReuseArgument.LastIncludedLog = new Binary(FirstLog.Encode());
                }

                var r = new InstallSnapshot() { Argument = ReuseArgument };
                var timeout = ls.Raft.RaftConfig.AppendEntriesTimeout;
                if (!r.Send(c.TryGetReadySocket(), (p) => ProcessResult(ls, c, p), timeout))
                {
                    ls.EndInstallSnapshot(c);
                }
            }
        }

        private long ProcessResult(LogSequence ls, Server.ConnectorEx c, Protocol p)
        {
            var r = p as InstallSnapshot;

            lock (ls.Raft)
            {
                if (r.IsTimeout)
                {
                    ls.EndInstallSnapshot(c);
                    return 0;
                }

                if (ls.TrySetTerm(r.Result.Term) == LogSequence.SetTermResult.Newer)
                {
                    ls.EndInstallSnapshot(c);
                    // new term found.
                    ls.Raft.ConvertStateTo(Raft.RaftState.Follower);
                    return 0; // break install
                }

                switch (r.ResultCode)
                {
                    case Procedure.Success:
                    case InstallSnapshot.ResultCodeNewOffset:
                        break;

                    default:
                        LogSequence.logger.Warn($"InstallSnapshot Break ResultCode={r.ResultCode}");
                        ls.EndInstallSnapshot(c);
                        return 0; // break install
                }

                if (r.Result.Offset >= 0)
                {
                    if (r.Result.Offset > File.Length)
                    {
                        LogSequence.logger.Error($"InstallSnapshot.Result.Offset Too Big.{r.Result.Offset}/{File.Length}");
                        ls.EndInstallSnapshot(c);
                        return 0; // 中断安装。
                    }
                    Offset = r.Result.Offset;
                    File.Seek(Offset, SeekOrigin.Begin);
                }
            }
            TrySend(ls, c);
            return 0;
        }
    }
}
