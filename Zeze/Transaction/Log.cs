
namespace Zeze.Transaction
{
    /// <summary>
    /// 操作日志。
    /// 主要用于 bean.variable 的修改。
    /// 用于其他非 bean 的日志时，也需要构造一个 bean，用来包装日志。
    /// </summary>
    public abstract class Log
    {
        public abstract void Commit();
        //public void Rollback() { } // 一般的操作日志不需要实现，特殊日志可能需要。先不实现，参见Savepoint.
        public abstract long LogKey { get; } // 日志key，由 Bean.ObjectId + Variable.Id 构成
        public Bean Bean { get; set; }
        public Log(Bean bean)
        {
            Bean = bean;
        }
        public int VariableId => (int)(LogKey & Bean.MaxVariableId);
    }

    public abstract class Log<TBean, TValue> : Log where TBean : Bean
    {
        public TValue Value { get; set; }

        protected Log(Bean bean, TValue value) : base(bean)
        {
            this.Value = value;
        }

        public TBean BeanTyped => (TBean)Bean;
    }
}