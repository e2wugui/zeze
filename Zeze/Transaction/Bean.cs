using System;
using System.Collections.Generic;
using System.Reflection;
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
            TypeId_ = Util.FixedHash.Hash64(GetType().FullName);
        }

        public Bean(int variableId)
        {
            this.VariableId = variableId;
            TypeId_ = Util.FixedHash.Hash64(GetType().FullName);
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

        public void InitRootInfoWithRedo(Record.RootInfo rootInfo, Bean parent)
        {
            if (IsManaged)
            {
                throw new HasManagedException();
            }
            this.RootInfo = rootInfo;
            this.Parent = parent;
            Transaction.WhileRedo(this);
            InitChildrenRootInfoWithRedo(rootInfo);
        }

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

        public void ResetRootInfo()
        {
            RootInfo = null;
            Parent = null;
        }

        // 用在第一次加载Bean时，需要初始化它的root
        protected virtual void InitChildrenRootInfo(Record.RootInfo root)
        {
        }

        protected virtual void InitChildrenRootInfoWithRedo(Record.RootInfo root)
        {
        }

        public abstract void ClearParameters();

        public abstract void Decode(global::Zeze.Serialize.ByteBuffer bb);
        public abstract void Encode(global::Zeze.Serialize.ByteBuffer bb);

        // helper
        public virtual int CapacityHintOfByteBuffer => 1024; // 生成工具分析数据结构，生成容量提示，减少内存拷贝。

        public virtual bool NegativeCheck()
        {
            return false;
        }

        public virtual Bean Copy()
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

        public static bool IsEmptyBean(Bean bean)
        {
            return bean.TypeId == EmptyBean.TYPEID;
        }

        public virtual long GetVersion()
        {
            return 0;
        }

        public virtual string GetVersionVarName()
        {
            return "";
        }

        protected virtual void SetVersion(long newValue)
        {
            // 子类重载
        }

        internal void SetVersionInternal(long newValue)
        {
            SetVersion(newValue);
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

        public override EmptyBean Copy()
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

        public override void ClearParameters()
        {
        }
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

                // 总是跟随Bean_一起设置，这里只需提供读取。
                var log = (LogDynamic)txn.GetLog(Parent.ObjectId + VariableId);
                return log != null ? log.SpecialTypeId : TypeId_;
            }
        }

        public object _zeze_map_key_ { get; set; } // 支持直接作为map的value。是一种优化！

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
                var log = (LogDynamic)txn.GetLog(Parent.ObjectId + VariableId);
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
                value.InitRootInfoWithRedo(RootInfo, this);
                value.VariableId = 1; // 只有一个变量
                var txn = Transaction.Current;
                txn.VerifyRecordAccessed(this);
                var log = (LogDynamic)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
                log.SetValue(value);
            }
        }

        internal Bean Bean_;

        public override void ClearParameters()
        {
            Bean.ClearParameters();
        }

        internal void SetTypeId(long typeId)
        {
            TypeId_ = typeId;
        }


        public Func<Bean, long> GetSpecialTypeIdFromBean { get; }
        public Func<long, Bean> CreateBeanFromSpecialTypeId { get; }

        public DynamicBean()
        {
            throw new NotImplementedException("use method in holder bean that user defined.");
        }

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
            Bean = other.Bean.Copy();
        }

        public override bool NegativeCheck()
        {
            return Bean.NegativeCheck();
        }

        public override int CapacityHintOfByteBuffer => Bean.CapacityHintOfByteBuffer;

        public override DynamicBean Copy()
        {
            var copy = new DynamicBean(VariableId, GetSpecialTypeIdFromBean, CreateBeanFromSpecialTypeId);
            copy.Bean_ = Bean.Copy();
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
            bean.InitRootInfoWithRedo(RootInfo, this);
            bean.VariableId = 1; // 只有一个变量
            var txn = Transaction.Current;
            txn.VerifyRecordAccessed(this);
            var log = (LogDynamic)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
            log.SetValue(bean);
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

        protected override void InitChildrenRootInfoWithRedo(Record.RootInfo root)
        {
            Bean_.InitRootInfoWithRedo(root, this);
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var dlog = (LogDynamic)log;
            if (null != dlog.Value)
            {
                // 内部Bean整个被替换。
                TypeId_ = dlog.SpecialTypeId;
                Bean_ = dlog.Value;
            }
            else if (null != dlog.LogBean) // 安全写法，不检查应该是没问题的？
            {
                // 内部Bean发生了改变。
                Bean_.FollowerApply(dlog.LogBean);
            }
        }

        public override LogBean CreateLogBean()
        {
            return new LogDynamic()
            {
                Belong = Parent,
                This = this,
                VariableId = VariableId,
            };
        }

        public override int GetHashCode()
        {
            return TypeId_.GetHashCode() ^ Bean_.GetHashCode();
        }

        protected bool Equals(DynamicBean other)
        {
            return TypeId_ == other.TypeId_ && Bean_.Equals(other.Bean_);
        }

        public override bool Equals(object obj)
        {
            if (ReferenceEquals(obj, null))
                return false;
            if (ReferenceEquals(obj, this))
                return true;
            return GetType() == obj.GetType() && Equals((DynamicBean)obj);
        }
    }

    // see Zeze.Util.ConfBean.cs::LogConfDynamic
    public class LogDynamic : LogBean
    {
        public readonly static new string StableName = Util.Reflect.GetStableName(typeof(LogDynamic));
        public readonly static new int TypeId_ = Util.FixedHash.Hash32(StableName);

        public override int TypeId => TypeId_;

        public long SpecialTypeId { get; private set; }
        public Bean Value { get; private set; }
        public LogBean LogBean { get; private set; }

        // 收集内部的Bean发生了改变。
        public override void Collect(Changes changes, Bean recent, Log vlog)
        {
            if (LogBean == null)
            {
                LogBean = (LogBean)vlog;
                changes.Collect(recent, this);
            }
        }

        internal override Log BeginSavepoint()
        {
            var dup = new LogDynamic();
            dup.This = This;
            dup.Belong = Belong;
            dup.VariableId = VariableId;
            dup.SpecialTypeId = SpecialTypeId;
            dup.Value = Value;
            return dup;
        }

        internal override void EndSavepoint(Savepoint currentsp)
        {
            // 结束保存点，直接覆盖到当前的日志里面即可。
            currentsp.PutLog(this);
        }

        public override void Commit()
        {
            if (Value != null)
            {
                var self = (DynamicBean)This;
                self.Bean_ = Value;
                self.SetTypeId(SpecialTypeId);
            }
        }

        public void SetValue(Bean bean)
        {
            Value = bean;
            var self = (DynamicBean)This;
            SpecialTypeId = self.GetSpecialTypeIdFromBean(bean);
        }

        public override void Encode(ByteBuffer bb)
        {
            // encode Value & SpecialTypeId. Value maybe null.
            var self = (DynamicBean)This;
            bb.WriteString(self.Parent.GetType().FullName); // use in decode reflect
            bb.WriteInt(self.VariableId);
            if (null != Value)
            {
                bb.WriteBool(true);
                bb.WriteLong(SpecialTypeId);
                Value.Encode(bb);
            }
            else
            {
                bb.WriteBool(false); // Value Tag
                if (null != LogBean)
                {
                    bb.WriteBool(true);
                    LogBean.Encode(bb);
                }
                else
                {
                    bb.WriteBool(false);
                }
            }
        }

        public override void Decode(ByteBuffer bb)
        {
            var parentTypeName = bb.ReadString();
            var variableId = bb.ReadInt();
            var hasValue = bb.ReadBool();
            if (hasValue)
            {
                SpecialTypeId = bb.ReadLong();
                var parentType = Zeze.Util.Reflect.GetType(parentTypeName);
                var factory = parentType.GetMethod("CreateBeanFromSpecialTypeId_" + variableId, BindingFlags.Static | BindingFlags.Public, new Type[] { typeof(long) });
                Value = (Bean)factory.Invoke(null, new object[] { SpecialTypeId });
                Value.Decode(bb);
            }
            else
            {
                var hasLogBean = bb.ReadBool();
                if (hasLogBean)
                {
                    LogBean = new LogBean();
                    LogBean.Decode(bb);
                }
            }
        }
    }
}
