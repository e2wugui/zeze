// auto-generated

using Zeze.Transaction;
using Zeze.Transaction.Collections;

namespace Zege
{
    public class FollowerApplyTables
    {
        public static void RegisterLog()
        {
            Log.Register<Log<int>>();
            Log.Register<Log<long>>();
            Log.Register<Log<string>>();
            Log.Register<Log<Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey>>();
            Log.Register<Log<Zeze.Net.Binary>>();
            Log.Register<LogList2<Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeValue>>();
            Log.Register<LogMap1<string, string>>();
            Log.Register<Zeze.Util.LogConfDynamic>();
        }
    }
}
