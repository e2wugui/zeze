using System;
using System.Collections.Generic;
using Zeze.Serialize;
using Zeze.Transaction.Collections;

namespace Zeze.Transaction
{
    /// <summary>
    /// 这个类描述了增量更新的Changes.Record的解码和FollowerApply过程。
    /// 编码参见Table.ChangeListenerEncodeWithTableName。
    /// 客户端数据存储通常更加灵活，而且需要更新UI，所以这个代码用处不大。
    /// 【建议】客户端的解码和FollowerApply参考这个代码完全自己实现一份。
    /// </summary>
    public class ChangesRecord
    {
        public const int Remove = 0;
        public const int Put = 1;
        public const int Edit = 2;

        public int State { get; set; }
        public Util.ConfBean Value { get; set; }
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
                    using (var it = LogBean.GetEnumerator())
                    {
                        if (it.MoveNext())
                        {
                            var existBean = table.Get(key);
                            existBean?.FollowerApply(it.Current);
                        }
                    }
                    break;
            }
        }
    }

    public interface ChangesTable
    {
        object DecodeKey(ByteBuffer bb);
        Util.ConfBean NewValueBean();
        Util.ConfBean Get(object key);
        void Put(object key, Util.ConfBean value);
        void Remove(object key);
    }
}
