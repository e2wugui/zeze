using System;
using System.Collections.Generic;
using System.Runtime.Serialization;
using Zeze.Serialize;
using Zeze.Transaction.Collections;

namespace Zeze.Transaction
{
    public abstract class Bean : global::Zeze.Serialize.Serializable
    {
        private static global::Zeze.Util.AtomicLong _objectIdGen = new();

        public const int ObjectIdStep = 4096; // 自增长步长。低位保留给Variable.Id。也就是，Variable.Id 最大只能是4095.
        public const int MaxVariableId = ObjectIdStep - 1;

        public long ObjectId { get; } = GetNextObjectId();
        public static long GetNextObjectId() { return _objectIdGen.AddAndGet(ObjectIdStep); }

        public Record.RootInfo RootInfo { get; private set; }
        public TableKey TableKey => RootInfo?.TableKey;
        // Parent VariableId 是 ChangeListener 需要的属性。
        // Parent 和 TableKey 一起初始化，仅在被Table管理以后才设置。
        public Bean Parent { get; private set; }
        // VariableId 初始化分两部分：
        // 1. Bean 包含的 Bean 在构造的时候初始化，同时初始化容器的LogKey（包含 VariableId）
        // 2. Bean 加入容器时，由容器初始化。使用容器所在Bean的LogKey中的VariableId初始化。
        public int VariableId { get; set; }

        public Bean()
        { 
            TypeId_ = Hash64(GetType().FullName);
        }

        public Bean(int variableId)
        {
            this.VariableId = variableId;
            TypeId_ = Hash64(GetType().FullName);
        }

        public virtual LogBean CreateLogBean()
        {
            return new LogBean() { Belong = Parent, This = this, VariableId = VariableId, };
        }

        public virtual void FollowerApply(Log log)
        {
            throw new NotImplementedException();
        }

        /// <summary>
        /// 构建 ChangeListener 链。其中第一个KeyValuePair在调用前加入，这个由Log或者ChangeNote提供。
        /// </summary>
        /// <param name="path"></param>
        /// <returns></returns>
        internal void BuildChangeListenerPath(List<Util.KV<Bean, int>> path)
        {
            for (Bean parent = Parent; parent != null; parent = parent.Parent)
            {
                path.Add(Util.KV.Create(parent, VariableId));
            }
        }

        public bool IsManaged => RootInfo != null;

        public void InitRootInfo(Record.RootInfo rootInfo, Bean parent)
        {
            if (IsManaged)
            {
                throw new HasManagedException();
            }
            this.RootInfo = rootInfo;
            this.Parent = parent;
            InitChildrenRootInfo(rootInfo);
        }

        // 用在第一次加载Bean时，需要初始化它的root
        protected abstract void InitChildrenRootInfo(Record.RootInfo root);

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

        public virtual void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(new string(' ', level)).Append("{}").Append(System.Environment.NewLine);
        }

        // Bean的类型Id，替换 ClassName，提高效率和存储空间
        // 用来支持 dynamic 类型，或者以后的扩展。
        // 默认实现是 ClassName.HashCode()，也可以手动指定一个值。
        // Gen的时候会全局判断是否出现重复冲突。如果出现冲突，则手动指定一个。
        // 这个方法在Gen的时候总是覆盖(override)，提供默认实现是为了方便内部Bean的实现。
        protected long TypeId_;
        public virtual long TypeId => TypeId_;

        // 使用自己的hash算法，因为 TypeId 会持久化，不能因为算法改变导致值变化。
        // XXX: 这个算法定好之后，就不能变了。
        public static long Hash64(string name)
        {
            // This is a Knuth hash
            ulong hashedValue = 3074457345618258791ul;
            for (int i = 0; i < name.Length; i++)
            {
                hashedValue += name[i];
                hashedValue *= 3074457345618258799ul;
            }
            return (long)hashedValue;
        }

        public static int Hash32(string name)
        {
            ulong hash64 = (ulong)Hash64(name);
            uint hash32 = (uint)(hash64 & 0xffffffff) ^ (uint)(hash64 >> 32);
            return (int)hash32;
        }

        public static bool IsEmptyBean(Bean bean)
        {
            return bean.TypeId == EmptyBean.TYPEID;
        }
    }

    public class EmptyBean : Bean
    {
        public override void Decode(ByteBuffer bb)
        {
            bb.ReadByte();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteByte(0);
        }

        protected override void InitChildrenRootInfo(Record.RootInfo root)
        {
        }

        public override Bean CopyBean()
        {
            return new EmptyBean();
        }

        public const long TYPEID = 0; // 用0，而不是Bean.Hash("")，可能0更好吧。

        public override long TypeId => TYPEID;

        public override string ToString()
        {
            return "()";
        }

        public override void FollowerApply(Log log)
        {
        }

        public readonly static EmptyBean Instance = new();
    }

    public interface DynamicBeanReadOnly
    { 
        public long TypeId { get; }
        public Bean Bean { get; }
    }

    public class DynamicBean : Bean, DynamicBeanReadOnly
    {
        public override long TypeId
        {
            get
            {
                if (false == this.IsManaged)
                    return TypeId_;
                var txn = Transaction.Current;
                if (txn == null)
                    return TypeId_;
                txn.VerifyRecordAccessed(this, true);
                // 不能独立设置，总是设置Bean时一起Commit，所以这里访问Bean的Log。
                var log = (DynamicLog)txn.GetLog(this.ObjectId + 1);
                return log != null ? log.SpecialTypeId : TypeId_;
            }
        }

        public Bean Bean
        {
            get
            {
                if (false == this.IsManaged)
                    return Bean_;
                var txn = Transaction.Current;
                if (txn == null)
                    return Bean_;
                txn.VerifyRecordAccessed(this, true);
                var log = (DynamicLog)txn.GetLog(this.ObjectId + 1);
                return log != null ? log.Value : Bean_;
            }

            set
            {
                if (null == value)
                    throw new System.ArgumentNullException(nameof(value));

                if (false == this.IsManaged)
                {
                    TypeId_ = GetSpecialTypeIdFromBean(value);
                    Bean_ = value;
                    return;
                }
                value.InitRootInfo(RootInfo, this);
                value.VariableId = 1; // 只有一个变量
                var txn = Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new DynamicLog(this, value));
            }
        }

        private Bean Bean_;

        public Func<Bean, long> GetSpecialTypeIdFromBean { get; }
        public Func<long, Bean> CreateBeanFromSpecialTypeId { get; }

        public DynamicBean(int variableId, Func<Bean, long> get, Func<long, Bean> create)
            : base(variableId)
        {
            Bean_ = new EmptyBean();
            TypeId_ = EmptyBean.TYPEID;

            GetSpecialTypeIdFromBean = get;
            CreateBeanFromSpecialTypeId = create;
        }

        public bool IsEmpty()
        {
            return TypeId_ == EmptyBean.TYPEID && Bean_.GetType() == typeof(EmptyBean);
        }

        public void Assign(DynamicBean other)
        {
            Bean = other.Bean.CopyBean();
        }

        public override bool NegativeCheck()
        {
            return Bean.NegativeCheck();
        }

        public override int CapacityHintOfByteBuffer => Bean.CapacityHintOfByteBuffer;

        public override Bean CopyBean()
        {
            var copy = new DynamicBean(VariableId, GetSpecialTypeIdFromBean, CreateBeanFromSpecialTypeId);
            copy.Bean_ = Bean.CopyBean();
            copy.TypeId_ = TypeId;
            return copy;
        }

        private void SetBeanWithSpecialTypeId(long specialTypeId, Bean bean)
        {
            if (false == this.IsManaged)
            {
                TypeId_ = specialTypeId;
                Bean_ = bean;
                return;
            }
            bean.InitRootInfo(RootInfo, this);
            bean.VariableId = 1; // 只有一个变量
            var txn = Transaction.Current;
            txn.VerifyRecordAccessed(this);
            txn.PutLog(new DynamicLog(specialTypeId, this, bean));
        }

        public override void Decode(ByteBuffer bb)
        {
            // 由于可能在事务中执行，这里仅修改Bean
            // TypeId 在 Bean 提交时才修改，但是要在事务中读到最新值，参见 TypeId 的 getter 实现。
            long typeId = bb.ReadLong();
            Bean real = CreateBeanFromSpecialTypeId(typeId);
            if (real != null)
            {
                real.Decode(bb);
                SetBeanWithSpecialTypeId(typeId, real);
            }
            else
            {
                bb.SkipUnknownField(ByteBuffer.BEAN);
                SetBeanWithSpecialTypeId(EmptyBean.TYPEID, new EmptyBean());
            }
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteLong(TypeId);
            Bean.Encode(bb);
        }

        protected override void InitChildrenRootInfo(Record.RootInfo root)
        {
            Bean_.InitRootInfo(root, this);
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            Bean_.FollowerApply(log);
        }

        private sealed class DynamicLog : Log<Bean>
        {
            public long SpecialTypeId { get; }

            public DynamicLog(DynamicBean self, Bean value)
            {
                Belong = self;
                Value = value;
                // 提前转换，如果是本Dynamic中没有配置的Bean，马上抛出异常。
                SpecialTypeId = self.GetSpecialTypeIdFromBean(value);
            }

            internal DynamicLog(long specialTypeId, DynamicBean self, Bean value)
            {
                Belong = self;
                Value = value;
                SpecialTypeId = specialTypeId;
            }

            public override void Commit()
            {
                var self = (DynamicBean)Belong;
                self.Bean_ = Value;
                self.TypeId_ = SpecialTypeId;
            }
        }
    }
}
