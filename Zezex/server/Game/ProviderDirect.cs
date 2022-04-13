using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Beans.ProviderDirect;
using Zeze.Net;
using Zeze.Transaction;

namespace Game
{
    public class ProviderDirect : Zeze.Arch.ProviderDirect
    {
        protected override async Task<long> ProcessTransmit(Protocol _p)
        {
			var p = _p as Transmit;
			Zeze.Serialize.Serializable parameter = null;
			if (0 == p.Argument.ParameterBeanName.Length)
			{
				if (!App.Instance.Game_Login.Onlines.TransmitParameterFactorys.TryGetValue(p.Argument.ParameterBeanName, out var factory))
					return ErrorCode(ErrorTransmitParameterFactoryNotFound);

				parameter = factory(p.Argument.ParameterBeanName);
			}
			App.Instance.Game_Login.Onlines.ProcessTransmit(
					p.Argument.Sender,
					p.Argument.ActionName,
					p.Argument.Roles.Keys,
					parameter);
			return Procedure.Success;
		}
	}
}
