using System;
using System.Runtime.Serialization;

namespace Zeze.Transaction
{
    public abstract class Bean
    {
        private static Zeze.Util.AtomicLong _objectIdGen = new Zeze.Util.AtomicLong();

        public long ObjectId { get; } = _objectIdGen.AddAndGet(1);
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
    }
}
