using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Arch
{
    public class ProviderLoad : ProviderLoadBase
    {
        public Online Online { get; }

        public ProviderLoad(Online online)
            : base(online.ProviderApp.Zeze)
        {
            Online = online;
        }

        public override LoadConfig GetLoadConfig()
        {
            return Online.ProviderApp.Distribute.LoadConfig;
        }

        public override int GetOnlineLocalCount()
        {
            return Online.LocalCount;
        }

        public override long GetOnlineLoginTimes()
        {
            return Online.LoginTimes;
        }

        public override string GetProviderIp()
        {
            return Online.ProviderApp.DirectIp;
        }

        public override int GetProviderPort()
        {
            return Online.ProviderApp.DirectPort;
        }
    }
}
