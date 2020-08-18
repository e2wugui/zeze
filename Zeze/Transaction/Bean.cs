using System;
using System.Runtime.Serialization;

namespace Zeze.Transaction
{
    public abstract class Bean : Zeze.Serialize.Serializable
    {
        private static Zeze.Util.AtomicLong _objectIdGen = new Zeze.Util.AtomicLong();

        public const int ObjectIdStep = 4096; // 自增长步长。低位保留给Variable.Id。也就是，Variable.Id 最大只能是4095.
        public const int MaxVariableId = ObjectIdStep - 1;

        public static long NextObjectId => _objectIdGen.AddAndGet(ObjectIdStep);

        public long ObjectId { get; } = NextObjectId;
        public TableKey TableKey { get; private set; }
        public bool IsManaged => TableKey != null;

        public void InitTableKey(TableKey tableKey)
        {
            if (this.TableKey != null)
            {
                throw new Exception("Has In Managed");
            }
            this.TableKey = tableKey;
            InitChildrenTableKey(tableKey);
        }

        // 用在第一次加载Bean时，需要初始化它的root
        protected abstract void InitChildrenTableKey(TableKey root);

        public abstract void Decode(Zeze.Serialize.ByteBuffer bb);
        public abstract void Encode(Zeze.Serialize.ByteBuffer bb);
        public virtual int CapacityHintOfByteBuffer => 1024; // 生成工具分析数据结构，生成容量提示，减少内存拷贝。
    }
}
