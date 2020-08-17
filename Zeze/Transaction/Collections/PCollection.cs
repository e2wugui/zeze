
namespace Zeze.Transaction.Collections
{
    public abstract class PCollection
    {
        public TableKey TableKey { get; private set; }
        public bool IsManaged => TableKey != null;
        public long LogKey { get; }

        protected PCollection(long logKey)
        {
            LogKey = logKey;
        }

        /// <summary>
        /// 初始化 tableKey.
        /// 当容器所在的 bean 第一次加入管理时，被调用初始化。
        /// </summary>
        /// <param name="root"></param>
        public void InitTableKey(TableKey tableKey)
        {
            if (this.TableKey != null)
                throw new System.Exception("Has In Managed");
            this.TableKey = tableKey;
            InitChildrenTableKey(tableKey);
        }


        protected abstract void InitChildrenTableKey(TableKey root);
    }
}
