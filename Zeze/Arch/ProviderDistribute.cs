using System;
using System.Collections.Concurrent;
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

        public ConcurrentDictionary<string, Zeze.Util.ConsistentHash<ServiceInfo>> ConsistentHashs = new();

        public void AddServer(Agent.SubscribeState state, ServiceInfo s)
        {
            var consistentHash = ConsistentHashs.GetOrAdd(s.ServiceName, key => new());
            consistentHash.Add(s.ServiceIdentity, s);
        }

        public void RemoveServer(Agent.SubscribeState state, ServiceInfo s)
        {
            if (ConsistentHashs.TryGetValue(s.ServiceName, out var consistentHash))
                consistentHash.Remove(s.ServiceIdentity, s);
        }

        public void ApplyServers(Agent.SubscribeState ass)
        {
            var consistentHash = ConsistentHashs.GetOrAdd(ass.ServiceName, key => new());
            var nodes = consistentHash.Nodes;
            var current = new HashSet<ServiceInfo>();
            foreach (var node in ass.ServiceInfos.SortedIdentity)
            {
                consistentHash.Add(node.ServiceIdentity, node);
                current.Add(node);
            }
            foreach (var node in nodes)
            {
                if (!current.Contains(node))
                    consistentHash.Remove(node.ServiceIdentity, node);
            }
        }

        public ServiceInfo ChoiceHash(Agent.SubscribeState providers, int hash)
        {
            /*
             ConsistentHash.Get 还没有实现。
            if (ConsistentHashs.TryGetValue(providers.ServiceName, out var consistentHash))
                return consistentHash.Get(hash);
            */

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
            if (serviceInfo.LocalState is not ProviderModuleState providerModuleState)
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
                if (list[i].LocalState is not ProviderModuleState providerModuleState)
                    continue;
                if (ProviderService.GetSocket(providerModuleState.SessionId)?.UserState is not ProviderSession ps)
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

        private readonly Zeze.Util.AtomicInteger FeedFullOneByOneIndex = new();

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
                    if (serviceinfo.LocalState is not ProviderModuleState providerModuleState)
                        continue;
                    // 这里发现关闭的服务，仅仅忽略.
                    if (ProviderService.GetSocket(providerModuleState.SessionId)?.UserState is not ProviderSession ps)
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

        public bool ChoiceProviderByServerId(string serviceNamePrefix, int moduleId, int serverId, out long provider)
        {
            var serviceName = MakeServiceName(serviceNamePrefix, moduleId);
            if (false == Zeze.ServiceManagerAgent.SubscribeStates.TryGetValue(
                serviceName, out var volatileProviders))
            {
                provider = 0;
                return false;
            }
            var si = volatileProviders.ServiceInfos.Find(serverId);
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
