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
        public InstallSnapshot Pending { get; } = new InstallSnapshot(); // 【注意】重用这个Rpc
        public RaftLog FirstLog { get; set; }
        public FileStream File { get; set; }
        public long Offset { get; set; }

        public async Task TrySend(LogSequence ls, Server.ConnectorEx c)
        {
            using var lockraft = await ls.Raft.Monitor.EnterAsync();

            if (false == ls.InstallSnapshotting.TryGetValue(c.Name, out _))
            {
                return; // 安装取消了。
            }

            if (Pending.Argument.Done || ls.Raft.IsShutdown || false == ls.Raft.IsLeader)
            {
                await ls.EndInstallSnapshot(c);
                return; // install done
            }

            c.AppendLogActiveTime = Util.Time.NowUnixMillis;

            var buffer = new byte[32 * 1024];
            int rc = File.Read(buffer);
            Pending.Argument.Offset = Offset;
            Pending.Argument.Data = new Binary(buffer, 0, rc);
            Pending.Argument.Done = rc < buffer.Length;
            Offset += rc;
            if (Pending.Argument.Done)
            {
                Pending.Argument.LastIncludedLog = new Binary(FirstLog.Encode());
            }

            var timeout = ls.Raft.RaftConfig.AppendEntriesTimeout;
            Pending.ResultCode = Procedure.ErrorSendFail;
            if (!Pending.Send(c.TryGetReadySocket(), async (p) => await ProcessResult(ls, c, p), timeout))
            {
                await ls.EndInstallSnapshot(c);
            }
        }

        private async Task<long> ProcessResult(LogSequence ls, Server.ConnectorEx c, Protocol p)
        {
            var r = p as InstallSnapshot;

            using var lockraft = await ls.Raft.Monitor.EnterAsync();
            if (r.IsTimeout)
            {
                await ls.EndInstallSnapshot(c);
                return 0;
            }

            if (await ls.TrySetTerm(r.Result.Term) == LogSequence.SetTermResult.Newer)
            {
                await ls.EndInstallSnapshot(c);
                // new term found.
                await ls.Raft.ConvertStateTo(Raft.RaftState.Follower);
                return 0; // break install
            }

            switch (r.ResultCode)
            {
                case Procedure.Success:
                case InstallSnapshot.ResultCodeNewOffset:
                    break;

                default:
                    await ls.EndInstallSnapshot(c);
                    return 0; // break install
            }

            if (false == r.Argument.Done && r.Result.Offset >= 0)
            {
                if (r.Result.Offset > File.Length)
                {
                    LogSequence.logger.Error($"InstallSnapshot.Result.Offset Too Big.{r.Result.Offset}/{File.Length}");
                    await ls.EndInstallSnapshot(c);
                    return 0; // 中断安装。
                }
                Offset = r.Result.Offset;
                File.Seek(Offset, SeekOrigin.Begin);
            }
            await TrySend(ls, c);
            return 0;
        }
    }
}
