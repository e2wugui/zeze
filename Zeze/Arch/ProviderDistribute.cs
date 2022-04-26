using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Services.ServiceManager;

namespace Zeze.Arch
{
    public class ProviderDistribute
    {
        public Application Zeze { get; set; }
        public LoadConfig LoadConfig { get; set; }
        public Zeze.Net.Service ProviderService { get; set; }

        public int MaxOnlineNew { get; set; } = 30;

        public static string MakeServiceName(string prefix, int moduleId)
        {
            return $"{prefix}{moduleId}";
        }

        public ServiceInfo ChoiceHash(Agent.SubscribeState providers, int hash)
        {
            var list = providers.ServiceInfos.SortedIdentity;
            if (list.Count == 0)
                return null;
            return list[hash % list.Count];
        }

        public bool ChoiceHash(Agent.SubscribeState providers, int hash, out long provider)
        {
            provider = 0;
            var serviceInfo = ChoiceHash(providers, hash);
            if (null == serviceInfo)
                return false;
            var providerModuleState = serviceInfo.LocalState as ProviderModuleState;
            if (null == providerModuleState)
                return false;
            provider = providerModuleState.SessionId;
            return true;
        }

        public bool ChoiceLoad(Agent.SubscribeState providers, out long provider)
        {
            provider = 0;

            var list = providers.ServiceInfos.SortedIdentity;
            var frees = new List<ProviderSession>(list.Count);
            var all = new List<ProviderSession>(list.Count);
            int TotalWeight = 0;

            // 新的provider在后面，从后面开始搜索。后面的可能是新的provider。
            for (int i = list.Count - 1; i >= 0; --i)
            {
                var providerModuleState = list[i].LocalState as ProviderModuleState;
                if (null == providerModuleState)
                    continue;
                var ps = ProviderService.GetSocket(providerModuleState.SessionId)?.UserState as ProviderSession;
                if (null == ps)
                    continue; // 这里发现关闭的服务，仅仅忽略.
                all.Add(ps);
                if (ps.Load.OnlineNew > MaxOnlineNew)
                    continue;
                int weight = ps.Load.ProposeMaxOnline - ps.Load.Online;
                if (weight <= 0)
                    continue;
                frees.Add(ps);
                TotalWeight += weight;
            }
            if (TotalWeight > 0)
            {
                int randweight = global::Zeze.Util.Random.Instance.Next(TotalWeight);
                foreach (var ps in frees)
                {
                    int weight = ps.Load.ProposeMaxOnline - ps.Load.Online;
                    if (randweight < weight)
                    {
                        provider = ps.SessionId;
                        return true;
                    }
                    randweight -= weight;
                }
            }
            // 选择失败，一般是都满载了，随机选择一个。
            if (all.Count > 0)
            {
                provider = all[global::Zeze.Util.Random.Instance.Next(all.Count)].SessionId;
                return true;
            }
            // no providers
            return false;
        }

        private Zeze.Util.AtomicInteger FeedFullOneByOneIndex = new Zeze.Util.AtomicInteger();

        public bool ChoiceFeedFullOneByOne(Agent.SubscribeState providers, out long provider)
        {
            // 查找时增加索引，和喂饱时增加索引，需要原子化。提高并发以后慢慢想，这里应该足够快了。
            lock (this)
            {
                provider = 0;

                var list = providers.ServiceInfos.SortedIdentity;
                // 最多遍历一次。循环里面 continue 时，需要递增索引。
                for (int i = 0; i < list.Count; ++i, FeedFullOneByOneIndex.IncrementAndGet())
                {
                    var index = (int)((uint)FeedFullOneByOneIndex.Get() % (uint)list.Count); // current
                    var serviceinfo = list[index];
                    var providerModuleState = serviceinfo.LocalState as ProviderModuleState;
                    if (providerModuleState == null)
                        continue;
                    var ps = ProviderService.GetSocket(providerModuleState.SessionId)?.UserState as ProviderSession;
                    // 这里发现关闭的服务，仅仅忽略.
                    if (null == ps)
                        continue;
                    // 这个和一个一个喂饱冲突，但是一下子给一个服务分配太多用户，可能超载。如果不想让这个生效，把MaxOnlineNew设置的很大。
                    if (ps.Load.OnlineNew > LoadConfig.MaxOnlineNew)
                        continue;

                    provider = ps.SessionId;
                    if (ps.Load.Online >= ps.Load.ProposeMaxOnline)
                        FeedFullOneByOneIndex.IncrementAndGet(); // 已经喂饱了一个，下一个。

                    return true;
                }
                return false;
            }
        }

        public ServiceInfo ChoiceProvider(string prefix, int moduleId, int hash)
        {
            var serviceName = MakeServiceName(prefix, moduleId);
            if (false == Zeze.ServiceManagerAgent.SubscribeStates.TryGetValue(serviceName, out var volatileProviders))
                return null;
            return ChoiceHash(volatileProviders, hash);
        }

        public bool ChoiceProvider(string prefix, int moduleId, int hash, out long provider)
        {
            provider = 0;
            var serviceInfo = ChoiceProvider(prefix, moduleId, hash);
            if (null == serviceInfo)
                return false;

            var providerModuleState = (ProviderModuleState)serviceInfo.LocalState;
            if (providerModuleState == null)
                return false;
            provider = providerModuleState.SessionId;
            return true;
        }

        public bool ChoiceProviderByServerId(string serviceNamePrefix, int moduleId, int hash, out long provider)
        {
            var serviceName = MakeServiceName(serviceNamePrefix, moduleId);
            if (false == Zeze.ServiceManagerAgent.SubscribeStates.TryGetValue(
                serviceName, out var volatileProviders))
            {
                provider = 0;
                return false;
            }
            var si = volatileProviders.ServiceInfos.FindServiceInfoByServerId(hash);
            if (null != si)
            {
                var state = (ProviderModuleState)si.LocalState;
                provider = state.SessionId;
                return true;
            }
            provider = 0;
            return false;
        }
    }
}
