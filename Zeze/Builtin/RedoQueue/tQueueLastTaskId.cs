// auto-generated
using Zeze.Serialize;
using Zeze.Transaction;
using System.Threading.Tasks;

namespace Zeze.Builtin.RedoQueue
{
    public sealed class tQueueLastTaskId : Table<string, Zeze.Builtin.RedoQueue.BTaskId>, TableReadOnly<string, Zeze.Builtin.RedoQueue.BTaskId, Zeze.Builtin.RedoQueue.BTaskIdReadOnly>
    {
        public tQueueLastTaskId() : base("Zeze_Builtin_RedoQueue_tQueueLastTaskId")
        {
        }

        public override int Id => -1495051256;
        public override bool IsMemory => false;
        public override bool IsAutoKey => false;

        public const int VAR_TaskId = 1;

        public override string DecodeKey(ByteBuffer _os_)
        {
            string _v_;
            _v_ = _os_.ReadString();
            return _v_;
        }

        public override ByteBuffer EncodeKey(string _v_)
        {
            ByteBuffer _os_ = ByteBuffer.Allocate();
            _os_.WriteString(_v_);
            return _os_;
        }

        async Task<Zeze.Builtin.RedoQueue.BTaskIdReadOnly> TableReadOnly<string, Zeze.Builtin.RedoQueue.BTaskId, Zeze.Builtin.RedoQueue.BTaskIdReadOnly>.GetAsync(string key)
        {
            return await GetAsync(key);
        }
    }
}
