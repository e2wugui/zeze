using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Beans.ProviderDirect;
using Zeze.Transaction;
using static Zeze.Net.Service;

namespace Zeze.Arch
{
    public class RedirectAllContext : Zeze.Net.Service.ManualContext
    {
        public string MethodFullName { get; }
        public HashSet<int> HashCodes { get; } = new();
        public ConcurrentDictionary<int, long> HashErrors { get; } = new();
        public Action<RedirectAllContext> OnHashEnd { get; set; }
        public Zeze.Net.Service Service { get; set; } // setup when add 

        public RedirectAllContext(int concurrentLevel, string methodFullName)
        {
            for (int hash = 0; hash < concurrentLevel; ++hash)
                HashCodes.Add(hash);
            MethodFullName = methodFullName;
        }

        public override void OnRemoved()
        {
            lock (this)
            {
                foreach (var hash in HashCodes)
                    HashErrors.TryAdd(hash, Procedure.Timeout);

                HashCodes.Clear();
                OnHashEnd?.Invoke(this);
                OnHashEnd = null;
            }
        }

        /// <summary>
        /// 调用这个方法处理hash分组结果，真正的处理代码在action中实现。
        /// 1) 在锁内执行；
        /// 2) 需要时初始化UserState并传给action；
        /// 3) 处理完成时删除Context
        /// </summary>
        public long ProcessHash<T>(int hash, Func<T> factory, Func<T, long> action)
        {
            lock (this)
            {
                try
                {
                    if (null == UserState)
                        UserState = factory();
                    return action((T)UserState);
                }
                finally
                {
                    HashCodes.Remove(hash); // 如果不允许一个hash分组处理措辞，把这个移到开头并判断结果。
                    if (HashCodes.Count == 0)
                    {
                        Service.TryRemoveManualContext<ManualContext>(SessionId);
                    }
                }
            }
        }

        // 这里处理真正redirect发生时，从远程返回的结果。
        public async Task ProcessResult(Application zeze, ModuleRedirectAllResult result)
        {
            foreach (var h in result.Argument.Hashs)
            {
                if (h.Value.ReturnCode != 0)
                {
                    HashErrors.TryAdd(h.Key, h.Value.ReturnCode);
                    continue;
                }
                // 不判断单个分组的处理结果，错误也继续执行其他分组。XXX
                await zeze.NewProcedure(async () => await ProcessHashResult(zeze, h.Key, h.Value.Params), MethodFullName).CallAsync();
            }
        }

        // 生成代码实现。see Game.ModuleRedirect.cs
        public virtual async Task<long> ProcessHashResult(Application zeze, int _hash_, Zeze.Net.Binary _params)
        {
            return Procedure.NotImplement;
        }
    }
}
