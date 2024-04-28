using DotNext.Collections.Generic;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Services.ServiceManager;

namespace Zeze.Arch
{
    public class ProviderDistributeVersion
    {
        public Application Zeze { get; set; }
        public LoadConfig LoadConfig { get; set; }
        public Zeze.Net.Service ProviderService { get; set; }

        public Dictionary<long, ProviderDistribute> DistributeVersions { get; } = new();

        public void AddServer(ServiceInfo info)
        {
            DistributeVersions.GetOrAdd(info.Version, _ => new ProviderDistribute(info.Version, Zeze, ProviderService, LoadConfig)).AddServer(info);
        }

        public void RemoveServer(ServiceInfo info)
        {
            if (DistributeVersions.TryGetValue(info.Version, out var versions))
            {
                versions.RemoveServer(info);
                // 有线程问题，干脆不删除，保留空的在里面吧。
                //if (DistributeVersions.Count == 0)
                //    DistributeVersions.Remove(info.Version);
            }
        }

        public ProviderDistribute SelectDistribute(long version)
        {
            // 更多选择算法
            if (DistributeVersions.TryGetValue(version, out var versions))
                return versions;
            return null;
        }
    }
}
