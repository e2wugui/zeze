
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

        public override int ProcessCEnterWorld(Protocol p)
        {
            var protocol = p as CEnterWorld;
            Game.Login.Session session = Game.Login.Session.Get(protocol);
            if (null == session.RoleId)
            {
                return Procedure.LogicError;
            }

            // TODO map
            return Zeze.Transaction.Procedure.NotImplement;
        }

        public override int ProcessCEnterWorldDone(Protocol _p)
        {
            var p = _p as CEnterWorldDone;
            // TODO map
            return Zeze.Transaction.Procedure.NotImplement;
        }
    }
}
