using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
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

        protected override async Task<long> ProcessSendConfirm(Protocol _p)
        {
            var p = _p as SendConfirm;
            var linkSession = (Zeze.Arch.ProviderService.LinkSession)p.Sender.UserState;
            var ctx = ProviderApp.ProviderService.TryGetManualContext<Online.ConfirmContext>(p.Argument.ConfirmSerialId);
            if (ctx != null)
            {
                ctx.ProcessLinkConfirm(linkSession.Name);
            }
            // linkName 也可以从 protocol.Sender.Connector.Name 获取。
            return Procedure.Success;
        }
    }
}
