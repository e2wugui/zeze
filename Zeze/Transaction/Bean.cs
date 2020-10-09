using System;
using System.Runtime.Serialization;
using Zeze.Serialize;

namespace Zeze.Transaction
{
    public abstract class Bean : global::Zeze.Serialize.Serializable
    {
        private static global::Zeze.Util.AtomicLong _objectIdGen = new global::Zeze.Util.AtomicLong();

        public const int ObjectIdStep = 4096; // 自增长步长。低位保留给Variable.Id。也就是，Variable.Id 最大只能是4095.
        public const int MaxVariableId = ObjectIdStep - 1;

        public static long NextObjectId => _objectIdGen.AddAndGet(ObjectIdStep);

        public long ObjectId { get; } = NextObjectId;
        public TableKey TableKey { get; private set; }
        public Bean Parent { get; private set; }
        public bool IsManaged => TableKey != null;

        public void InitTableKey(TableKey tableKey)
        {
            if (this.TableKey != null)
            {
                throw new HasManagedException();
            }
            this.TableKey = tableKey;
            InitChildrenTableKey(tableKey);
        }

        // 用在第一次加载Bean时，需要初始化它的root
        protected abstract void InitChildrenTableKey(TableKey root);

        public abstract void Decode(global::Zeze.Serialize.ByteBuffer bb);
        public abstract void Encode(global::Zeze.Serialize.ByteBuffer bb);

        // helper
        public virtual int CapacityHintOfByteBuffer => 1024; // 生成工具分析数据结构，生成容量提示，减少内存拷贝。
        public virtual bool NegativeCheck()
        {
            return false;
        }
        public virtual Bean CopyBean()
        {
            throw new NotImplementedException();
        }

        // Bean的类型Id，替换 ClassName，提高效率和存储空间
        // 用来支持 dynamic 类型，或者以后的扩展。
        // 默认实现是 ClassName.HashCode()，也可以手动指定一个值。
        // Gen的时候会全局判断是否出现重复冲突。如果出现冲突，则手动指定一个。
        // 这个方法在Gen的时候总是覆盖(override)，提供默认实现是为了方便内部Bean的实现。
        public virtual long TypeId => Hash64(GetType().FullName);

        // 使用自己的hash算法，因为 TypeId 会持久化，不能因为算法改变导致值变化。
        // XXX: 这个算法定好之后，就不能变了。
        public static long Hash64(string name)
        {
            // This is a Knuth hash
            UInt64 hashedValue = 3074457345618258791ul;
            for (int i = 0; i < name.Length; i++)
            {
                hashedValue += name[i];
                hashedValue *= 3074457345618258799ul;
            }
            return (long)hashedValue;
        }

        public static ushort Hash16(string protocolName)
        {
            ulong hash64 = (ulong)Hash64(protocolName);
            uint hash32 = (uint)(hash64 & 0xffffffff) ^ (uint)(hash64 >> 32);
            ushort hash16 = (ushort)((hash32 & 0xffff) ^ (hash32 >> 16));
            return hash16;
        }
    }

    public class EmptyBean : Bean
    {
        public override void Decode(ByteBuffer bb)
        {
        }

        public override void Encode(ByteBuffer bb)
        {
        }

        protected override void InitChildrenTableKey(TableKey root)
        {
        }

        public override Bean CopyBean()
        {
            return new EmptyBean();
        }

        public const long TYPEID = 0; // 用0，而不是Bean.Hash("")，可能0更好吧。

        public override long TypeId => TYPEID;
    }
}
