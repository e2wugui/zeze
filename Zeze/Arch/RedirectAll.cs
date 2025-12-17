using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Builtin.ProviderDirect;
using Zeze.Transaction;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Util;
using DotNext.Threading;
using System.Threading;

namespace Zeze.Arch
{
    public abstract class AbstractRedirectAll : Service.ManualContext
    {
        public abstract Task ProcessResult(Application zeze, ModuleRedirectAllResult result);
    }

    public class RedirectAll<T> : AbstractRedirectAll
    {
        public string MethodFullName { get; }
        public HashSet<int> HashCodes { get; } = new();
        public Dictionary<int, long> HashErrors { get; } = new();
        public Dictionary<int, T> HashResults { get; } = new();
        public TaskCompletionSource<RedirectAll<T>> Future { get; }
        public Func<Binary, T> ResultDecoder { get; }
        public bool IsCompleted => HashCodes.Count == 0;
        public Func<RedirectAll<T>, Task> Processing { get; }
        private AsyncLock Mutex = AsyncLock.Exclusive();

        public RedirectAll(
            int concurrentLevel, string methodFullName,
            Func<Binary, T> resultDecoder,
            Func<RedirectAll<T>, Task> processing = null)
        {
            for (int hash = 0; hash < concurrentLevel; ++hash)
                HashCodes.Add(hash);

            MethodFullName = methodFullName;
            Future = new();
            ResultDecoder = resultDecoder;
            Processing = processing;
        }

        public override async void OnRemoved()
        {
            using (await Mutex.AcquireAsync(CancellationToken.None))
            {
                foreach (var hash in HashCodes)
                {
                    HashErrors.TryAdd(hash, ResultCode.Timeout);
                }
                HashCodes.Clear();
                Future.TrySetResult(this);
            }
        }

        // 这里处理真正redirect发生时，从远程返回的结果。
        public override async Task ProcessResult(Application zeze, ModuleRedirectAllResult result)
        {
            // 一批结果锁一次。
            // 【注意】锁内回调 Processing.
            using (await Mutex.AcquireAsync(CancellationToken.None))
            {
                try
                {
                    // 处理结果，包括错误
                    foreach (var h in result.Argument.Hashs)
                    {
                        if (h.Value.ReturnCode == 0)
                        {
                            HashResults.Add(h.Key, ResultDecoder(h.Value.Params));
                        }
                        else
                        {
                            HashErrors.TryAdd(h.Key, h.Value.ReturnCode);
                        }
                    }
                    // 如果需要，部分结果回调。总是在事务中回调。
                    if (null != Processing)
                    {
                        await zeze.NewProcedure(async () =>
                        {
                            await Processing(this);
                            return 0;
                        },
                        MethodFullName).CallAsync();
                    }
                }
                finally
                {
                    // 移除处理过的hash，并且判断是否全部结束。
                    foreach (var hash in result.Argument.Hashs.Keys)
                    {
                        HashCodes.Remove(hash);
                    }
                    if (IsCompleted)
                    {
                        Service.TryRemoveManualContext<Service.ManualContext>(SessionId);
                    }
                }
            }
        }
    }

    // 不需要结果
    public class RedirectAll : RedirectAll<Gen.VoidResult>
    {
        public RedirectAll(
            int concurrentLevel, string methodFullName,
            Func<Binary, Gen.VoidResult> resultDecoder,
            Func<RedirectAll<Gen.VoidResult>, Task> processing = null)
            : base(concurrentLevel, methodFullName, resultDecoder, processing)
        {
        }
    }
}
