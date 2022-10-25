// auto-generated
using Zeze.Serialize;
using Zeze.Transaction;
using System.Threading.Tasks;

namespace Zeze.Builtin.Collections.Queue
{
    public sealed class tQueueNodes : Table<Zeze.Builtin.Collections.Queue.BQueueNodeKey, Zeze.Builtin.Collections.Queue.BQueueNode>, TableReadOnly<Zeze.Builtin.Collections.Queue.BQueueNodeKey, Zeze.Builtin.Collections.Queue.BQueueNode, Zeze.Builtin.Collections.Queue.BQueueNodeReadOnly>
    {
        public tQueueNodes() : base("Zeze_Builtin_Collections_Queue_tQueueNodes")
        {
        }

        public override int Id => -117984600;
        public override bool IsMemory => false;
        public override bool IsAutoKey => false;

        public const int VAR_NextNodeId = 1;
        public const int VAR_Values = 2;

        public override Zeze.Builtin.Collections.Queue.BQueueNodeKey DecodeKey(ByteBuffer _os_)
        {
            var _v_ = new Zeze.Builtin.Collections.Queue.BQueueNodeKey();
            _v_.Decode(_os_);
            return _v_;
        }

        public override ByteBuffer EncodeKey(Zeze.Builtin.Collections.Queue.BQueueNodeKey _v_)
        {
            ByteBuffer _os_ = ByteBuffer.Allocate();
            _v_.Encode(_os_);
            return _os_;
        }

        async Task<Zeze.Builtin.Collections.Queue.BQueueNodeReadOnly> TableReadOnly<Zeze.Builtin.Collections.Queue.BQueueNodeKey, Zeze.Builtin.Collections.Queue.BQueueNode, Zeze.Builtin.Collections.Queue.BQueueNodeReadOnly>.GetAsync(Zeze.Builtin.Collections.Queue.BQueueNodeKey key)
        {
            return await GetAsync(key);
        }
    }
}
