using System;
using System.Collections.Concurrent;
using System.Threading.Tasks;
using Zeze.Builtin.AutoKey;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Transaction;

namespace Zeze.Component
{
    public class AutoKey : AbstractAutoKey
    {
        public class Module : AbstractAutoKey
        {
            private readonly ConcurrentDictionary<string, AutoKey> map = new();
            public Application Zeze { get; }

            // 这个组件Zeze.Application会自动初始化，不需要应用初始化。
            public Module(Application zeze)
            {
                Zeze = zeze;
                RegisterZezeTables(zeze);
            }

            public override void UnRegister()
            {
                UnRegisterZezeTables(Zeze);
            }

            /**
			 * 这个返回值，可以在自己模块内保存下来，效率高一些。
			 */
            public AutoKey GetOrAdd(string name)
            {
                return map.GetOrAdd(name, name2 => new AutoKey(this, name2));
            }
        }

        private const int ALLOCATE_COUNT_MIN = 64;
        private const int ALLOCATE_COUNT_MAX = 1024 * 1024;

        private readonly Module module;
        private readonly string name;
        private readonly long logKey;
        private volatile Range range;
        private int allocateCount = ALLOCATE_COUNT_MIN;
        private long lastAllocateTime = Util.Time.NowUnixMillis;

        private AutoKey(Module module, string name)
        {
            this.module = module;
            this.name = name;

            logKey = Bean.GetNextObjectId();
        }

        public int GetAllocateCount()
        {
            return allocateCount;
        }

        public async Task<long> NextIdAsync()
        {
            var bb = await NextByteBufferAsync();
            if (bb.Size > 8)
                throw new Exception("out of range");
            return ByteBuffer.ToLongBE(bb.Bytes, bb.ReadIndex, bb.Size);
        }

        public async Task<byte[]> NextBytesAsync()
        {
            return (await NextByteBufferAsync()).Copy();
        }

        public async Task<Binary> NextBinaryAsync()
        {
            return new Binary(await NextByteBufferAsync());
        }

        public async Task<string> NextStringAsync()
        {
            var bb = await NextByteBufferAsync();
            return Convert.ToBase64String(bb.Bytes, bb.ReadIndex, bb.Size);
        }

        public async Task<ByteBuffer> NextByteBufferAsync()
        {
            var serverId = module.Zeze.Config.ServerId;
            var bb = ByteBuffer.Allocate(8);
            if (serverId > 0) // 如果serverId==0,写1个字节0不会影响ToLongBE的结果,但会多占1个字节,所以只在serverId>0时写ByteBuffer
                bb.WriteInt(serverId);
            else if (serverId < 0) // serverId不应该<0,因为会导致nextId返回负值
                throw new Exception("serverId(" + serverId + ") < 0");
            bb.WriteLong(await NextSeedAsync());
            return bb;
        }

        private async Task<long> NextSeedAsync()
        {
            if (null != range)
            {
                var next = range.TryNextId();
                if (next != null)
                    return next.Value; // allocate in range success
            }

            Transaction.Transaction.WhileCommit(() =>
            {
                // 不能在重做时重复计算，一次事务重新计算一次，下一次生效。
                // 这里可能有并发问题, 不过影响可以忽略
                var now = Util.Time.NowUnixMillis;
                var diff = now - lastAllocateTime;
                lastAllocateTime = now;
                long newCount = allocateCount;
                if (diff < 30 * 1000) // 30 seconds
                    newCount <<= 1;
                else if (diff > 120 * 1000) // 120 seconds
                    newCount >>= 1;
                else
                    return;
                allocateCount = (int)Math.Min(Math.Max(newCount, ALLOCATE_COUNT_MIN), ALLOCATE_COUNT_MAX);
            });

            var seedKey = new BSeedKey(module.Zeze.Config.ServerId, name);
            var txn = Transaction.Transaction.Current;
            var log = (RangeLog)txn.GetLog(logKey);
            while (true)
            {
                if (null == log)
                {
                    // allocate: 多线程，多事务，多服务器（缓存同步）由zeze保证。
                    var key = await module._tAutoKeys.GetOrAddAsync(seedKey);
                    var start = key.NextId;
                    var end = start + allocateCount; // allocateCount == 0 会死循环。
                    key.NextId = end;
                    // create log，本事务可见，
                    log = new RangeLog(this, new Range(start, end));
                    txn.PutLog(log);
                }

                var tryNext = log.range.TryNextId();
                if (tryNext != null)
                    return tryNext.Value;

                // 事务内分配了超出Range范围的id，再次allocate。
                // 覆盖RangeLog是可以的。就像事务内多次改变变量。最后面的Log里面的数据是最新的。
                // 已分配的范围保存在_AutoKeys表内，事务内可以继续分配。
                log = null;
            }
        }

        private class Range : Util.AtomicLong
        {
            private readonly long max;

            public long? TryNextId()
            {
                // 每次都递增。超出范围以后，也不恢复。
                var next = IncrementAndGet(); // 可能会超过max,但通常不会超出很多,更不可能溢出long最大值
                return next <= max ? next : null;
            }

            // 分配范围: [start+1,end]
            public Range(long start, long end) : base(start)
            {
                max = end;
            }
        }

        private class RangeLog : Log
        {
            private readonly AutoKey AutoKey;
            internal readonly Range range;

            public RangeLog(AutoKey autoKey, Range range)
            {
                AutoKey = autoKey;
                this.range = range;
            }

            public override long LogKey => AutoKey.logKey;

            public override void Commit()
            {
                // 这里直接修改拥有者的引用，开放出去，以后其他事务就能看到新的Range了。
                // 并发：多线程实际上由 _autokeys 表的锁来达到互斥，commit的时候，是互斥锁。
                AutoKey.range = range;
            }

            public override void Encode(ByteBuffer bb)
            {
                throw new NotImplementedException();
            }

            public override void Decode(ByteBuffer bb)
            {
                throw new NotImplementedException();
            }

            internal override void EndSavepoint(Savepoint currentSp)
            {
                currentSp.Logs[LogKey] = this;
            }

            internal override Log BeginSavepoint()
            {
                return this;
            }
        }
    }
}
