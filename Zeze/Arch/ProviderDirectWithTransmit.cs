using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Arch;
using Zeze.Builtin.ProviderDirect;
using Zeze.Net;
using Zeze.Transaction;
using Zeze.Util;

namespace Zeze.Arch
{
    public class ProviderDirectWithTransmit : ProviderDirect
    {
        protected override Task<long> ProcessTransmitAccount(Protocol _p)
        {
			var p = _p as TransmitAccount;
			var provider = ProviderApp.ProviderImplement as ProviderImplementWithOnline;
			provider.Online.ProcessTransmit(p.Argument.SenderAccount, p.Argument.SenderClientId,
				p.Argument.ActionName, p.Argument.TargetAccounts, p.Argument.Parameter);
			return Task.FromResult(ResultCode.Success);
		}
	}
}
