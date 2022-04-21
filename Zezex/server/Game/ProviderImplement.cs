using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Builtin.Provider;
using Zeze.Net;
using Zeze.Transaction;

namespace Game
{
    public class ProviderImplement : Zeze.Arch.ProviderImplement
    {
        protected override async Task<long> ProcessLinkBroken(Protocol _p)
        {
            var p = _p as LinkBroken;
            // 目前仅需设置online状态。
            if (0 == p.Argument.States.Count)
            {
                var roleId = p.Argument.States[0];
                await Game.App.Instance.Game_Login.Onlines.OnLinkBroken(roleId);
            }
            return Procedure.Success;
        }

        protected override async Task<long> ProcessSendConfirm(Protocol _p)
        {
            var p = _p as SendConfirm;
            var linkSession = (Zeze.Arch.ProviderService.LinkSession)p.Sender.UserState;
            var ctx = App.Instance.Server.TryGetManualContext<Game.Login.Onlines.ConfirmContext>(p.Argument.ConfirmSerialId);
            if (ctx != null)
            {
                ctx.ProcessLinkConfirm(linkSession.Name);
            }
            // linkName 也可以从 protocol.Sender.Connector.Name 获取。
            return Procedure.Success;
        }
    }
}
