
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

        public override int ProcessCEnterWorld(CEnterWorld protocol)
        {
            Game.Login.Session session = Game.Login.Session.Get(protocol);
            if (null == session.LoginRoleId)
            {
                return Zeze.Transaction.Procedure.LogicError;
            }

            // TODO map
            return Zeze.Transaction.Procedure.NotImplement;
        }

        public override int ProcessCEnterWorldDone(CEnterWorldDone protocol)
        {
            // TODO map
            return Zeze.Transaction.Procedure.NotImplement;
        }
    }
}
