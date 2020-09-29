
namespace Game.Bag
{
    public sealed partial class Module : AbstractModule
    {
        public void Start(Game.App app)
        {
        }

        public void Stop(Game.App app)
        {
        }

        // protocol handles
        public override int ProcessCMove(CMove protocol)
        {
            Login.Session session = Login.Session.Get(protocol);

            // throw if not login
            GetBag(session.LoginRoleId.Value).Move(protocol.Argument.PositionFrom, protocol.Argument.PositionTo, protocol.Argument.Number);

            return Zeze.Transaction.Procedure.Success;
        }

        public override int ProcessCDestroy(CDestroy protocol)
        {
            throw new System.NotImplementedException();
        }

        public override int ProcessCSort(CSort protocol)
        {
            throw new System.NotImplementedException();
        }

        public Bag GetBag(long roleid)
        {
            return new Bag(roleid, _tbag);
        }
    }
}
