using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Arch;
using Zeze.Builtin.Provider;
using Zeze.Net;
using Zeze.Transaction;

namespace Zeze.Arch
{
    public class ProviderImplementWithOnline : Zeze.Arch.ProviderImplement
    {
        public Online Online { get; set; }

        protected override async Task<long> ProcessLinkBroken(Protocol _p)
        {
            var p = _p as LinkBroken;
            // 目前仅需设置online状态。
            if (false == string.IsNullOrEmpty(p.Argument.Context))
            {
                await Online.OnLinkBroken(p.Argument.Account, p.Argument.Context, p.Argument.LinkSid, ProviderService.GetLinkName(p.Sender));
            }
            return Procedure.Success;
        }
    }
}
