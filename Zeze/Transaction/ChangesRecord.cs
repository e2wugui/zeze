using System;
using System.Collections.Generic;
using Zeze.Serialize;
using Zeze.Transaction.Collections;

namespace Zeze.Transaction
{
    public class ChangesRecord
    {
        // 编码参见Table.ChangeListenerEncodeWithTableName

        public const int Remove = 0;
        public const int Put = 1;
        public const int Edit = 2;

        public int State { get; set; }
#if USE_CONFCS
        public Zeze.Util.ConfBean Value { get; set; }
#else
        public Zeze.Transaction.Bean Value { get; set; }
#endif
        public ISet<LogBean> LogBean { get; } = new HashSet<LogBean>();

        // 解码辅助方法，
        // 这是c#客户端版本，其他版本可以参考这个自己实现。
        // 这个类用于 conf+cs+net，需要以拷贝源码的方式发布。

        public static void FollowerApply(ByteBuffer bb, Func<string, ChangesTable> GetTable)
        {
            var tableName = bb.ReadString();
            var table = GetTable(tableName);
            var key = table.DecodeKey(bb.ReadByteBuffer());
            new ChangesRecord().FollowerApply(table, key, bb);
        }

        private void FollowerApply(ChangesTable table, object key, ByteBuffer bb)
        {
            State = bb.ReadInt();
            switch (State)
            {
                case Remove:
                    table.Remove(key);
                    break;

                case Put:
                    Value = table.NewValueBean();
                    Value.Decode(bb);
                    table.Put(key, Value);
                    break;

                case Edit:
                    bb.Decode(LogBean);
                    var it = LogBean.GetEnumerator();
                    if (it.MoveNext())
                    {
                        var existBean = table.Get(key);
                        existBean?.FollowerApply(it.Current);
                    }
                    break;
            }
        }
    }

    public interface ChangesTable
    {
        public object DecodeKey(ByteBuffer bb);
#if USE_CONFCS
    public Zeze.Util.ConfBean NewValueBean();
    public Zeze.Util.ConfBean Get(object key);
    public void Put(object key, Zeze.Util.ConfBean value);
#else
        public Zeze.Transaction.Bean NewValueBean();
        public Zeze.Transaction.Bean Get(object key);
        public void Put(object key, Zeze.Transaction.Bean value);
#endif
        public void Remove(object key);
    }
}
