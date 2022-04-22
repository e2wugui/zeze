using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Arch;
using Zeze.Builtin.ProviderDirect;
using Zeze.Net;
using Zeze.Transaction;

namespace Zeze.Game
{
    public class ProviderDirectWithTransmit : ProviderDirect
    {
        protected override async Task<long> ProcessTransmit(Protocol _p)
        {
			var p = _p as Transmit;
			var provider = ProviderApp.ProviderImplement as ProviderImplementWithOnline;
			provider.Online.ProcessTransmit(p.Argument.Sender, p.Argument.ActionName,
				p.Argument.Roles.Keys, p.Argument.ParameterBeanValue);
			return Procedure.Success;
		}
	}
}
