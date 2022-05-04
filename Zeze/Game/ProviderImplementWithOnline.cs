using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Arch;
using Zeze.Builtin.Provider;
using Zeze.Net;
using Zeze.Transaction;

namespace Zeze.Game
{
    public class ProviderImplementWithOnline : Zeze.Arch.ProviderImplement
    {
        public Online Online { get; set; }

        protected override async Task<long> ProcessLinkBroken(Protocol _p)
        {
            var p = _p as LinkBroken;
            // 目前仅需设置online状态。
            if (p.Argument.States.Count > 0)
            {
                var roleId = p.Argument.States[0];
                await Online.OnLinkBroken(roleId, p.Argument);
            }
            return Procedure.Success;
        }
    }
}
