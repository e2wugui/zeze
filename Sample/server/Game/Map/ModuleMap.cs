
using System.Threading.Tasks;
using Zeze.Arch;
using Zeze.Net;
using Zeze.Transaction;

namespace Game.Map
{
    public sealed partial class ModuleMap : AbstractModule
    {
        public void Start(Game.App app)
        {
        }

        public void Stop(Game.App app)
        {
        }

        protected override Task<long> ProcessCEnterWorld(Protocol p)
        {
            var protocol = p as CEnterWorld;
            var session = ProviderUserSession.Get(protocol);
            if (null == session.RoleId)
            {
                return Task.FromResult(Procedure.LogicError);
            }

            // TODO map
            return Task.FromResult(Zeze.Transaction.Procedure.NotImplement);
        }

        protected override Task<long> ProcessCEnterWorldDone(Protocol _p)
        {
            //var p = _p as CEnterWorldDone;
            // TODO map
            return Task.FromResult(Zeze.Transaction.Procedure.NotImplement);
        }
    }
}
