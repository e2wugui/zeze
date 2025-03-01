﻿using System;
using System.Collections.Concurrent;
using System.Threading.Tasks;
using Zeze.Util;

namespace Zeze.Raft
{
    public abstract class StateMachine
    {
        private static readonly ILogger logger = LogManager.GetLogger(typeof(StateMachine));

        public Raft Raft { get; internal set; }

        public StateMachine()
        {
            AddFactory(new HeartbeatLog().TypeId, () => new HeartbeatLog());
        }

        private readonly ConcurrentDictionary<int, Func<Log>> LogFactorys = new();

        
        // 建议在继承类的构造里面注册LogFactory。
        protected void AddFactory(int logTypeId, Func<Log> factory)
        {
            if (!LogFactorys.TryAdd(logTypeId, factory))
                throw new Exception("Duplicate Log Id");
        }

        public virtual Log LogFactory(int logTypeId)
        {
            if (LogFactorys.TryGetValue(logTypeId, out var factory))
            {
                return factory();
            }
            logger.Fatal($"Unknown Log {logTypeId}");
            Raft.FatalKill();
            return null;
        }

        /// <summary>
        /// 把 StateMachine 里面的数据系列化到 path 指定的文件中。
        /// 需要自己访问的并发特性。返回快照建立时的Raft.LogSequence.Index。
        /// 原子性建议伪码如下：
        /// lock (Raft) // 这会阻止对 StateMachine 的写请求。
        /// {
        ///     var lastAppliedLog = Raft.LogSequence.LastAppliedLog();
        ///     LastIncludedIndex = lastAppliedLog.Index;
        ///     LastIncludedTerm = lastAppliedLog.Term;
        ///     MyData.SerializeToFile(path);
        ///     Raft.LogSequence.CommitSnapshot(path, LastIncludedIndex);
        /// }
        ///
        /// 上面的问题是，数据很大时，SerializeToFile时间比较长，会导致服务不可用。
        /// 这时候需要自己优化并发。如下：
        /// lock (Raft)
        /// {
        ///     var lastAppliedLog = Raft.LogSequence.LastAppliedLog();
        ///     LastIncludedIndex = lastAppliedLog.Index;
        ///     LastIncludedTerm = lastAppliedLog.Term;
        ///     // 设置状态，如果限制只允许一个snapshot进行，
        ///     // 新进的snapshot调用返回false。
        ///     MyData.StartSerializeToFile();
        /// }
        /// MyData.ConcurrentSerializeToFile(path);
        /// lock (Raft)
        /// {
        ///     // 清理一些状态。
        ///     MyData.EndSerializeToFile();
        ///     Raft.LogSequence.CommitSnapshot(path, LastIncludedIndex);
        /// }
        ///
        /// return true;
        ///
        /// 这样在保存数据到文件的过程中，服务可以继续进行。
        /// </summary>
        /// <param name="path"></param>
        /// <returns>(bool, term, index)</returns>
        public abstract Task<(bool, long, long)> Snapshot(string path);

        /// <summary>
        /// 从上一个快照中重建 StateMachine。
        /// Raft 处理 InstallSnapshot 到达最后一个数据时，调用这个方法。
        /// 然后 Raft 会从 LastIncludedIndex 后面开始复制日志。进入正常的模式。
        /// </summary>
        /// <param name="path"></param>
        public abstract Task LoadSnapshot(string path);
    }
}
