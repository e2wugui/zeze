using System;
using System.CodeDom.Compiler;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
using Zeze.Arch.Rpc;

namespace Zezex
{
    /// <summary>
    /// 把模块的方法调用发送到其他服务器实例上执行。
    /// 被重定向的方法用注解标明。
    /// 被重定向的方法需要是virtual的。
    /// 实现方案：
    /// Game.App创建Module的时候调用回调。
    /// 在回调中判断是否存在需要拦截的方法。
    /// 如果需要就动态生成子类实现代码并编译并返回新的实例。
    ///
    /// 注意：
    /// 使用 virtual override 的方式可以选择拦截部分方法。
    /// 可以提供和原来模块一致的接口。
    /// </summary>
    public class ModuleRedirect
    {
        // 本应用：hash分组的一些配置。
        public const int ChoiceType = Zezex.Provider.BModule.ChoiceTypeHashAccount;
        public static int GetChoiceHashCode()
        {
            string account = ((Game.Login.Session)Zeze.Transaction.Transaction.Current.TopProcedure.UserState).Account;
            return Zeze.Serialize.ByteBuffer.calc_hashnr(account);
        }

        public static Zeze.Net.AsyncSocket RandomLink()
        {
            return Game.App.Instance.Server.RandomLink();
        }

        public static ModuleRedirect Instance = new ModuleRedirect();


        // loopback 优化。
        public bool IsLocalServer(string moduleName)
        {
            // 要实现真正的 loopback，
            // 需要实现server-server之间直连并且可以得到当前的可用服务。
            // 通过linkd转发时，当前server没有足够信息做这个优化。
            return false;
        }

        // TODO REMOVE ME
        public Dictionary<string,
            Func<long, int, Zeze.Net.Binary, IList<Zezex.Provider.BActionParam>,
            (long, Zeze.Net.Binary)>
            > Handles
        { get; }
            = new Dictionary<string, Func<
                long, int, Zeze.Net.Binary,
                IList<Zezex.Provider.BActionParam>,
                (long, Zeze.Net.Binary)>>();

        private ModuleRedirect()
        {
        }
    }
}
