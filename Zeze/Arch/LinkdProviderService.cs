using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Arch
{
    public class LinkdProviderService : Zeze.Services.HandshakeServer
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        public LinkdApp LinkdApp { get; set; }
        public ConcurrentDictionary<string, ProviderSession> ProviderSessions = new();

        public LinkdProviderService(string name, Application zeze)
            : base(name, zeze)
        {

        }

    }
}
