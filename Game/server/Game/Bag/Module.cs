
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

            SChanged result = new SChanged();
            // throw exception if not login
            GetBag(session.LoginRoleId.Value).Move(protocol.Argument.PositionFrom, protocol.Argument.PositionTo, protocol.Argument.Number, result.Argument);
            session.SendResponse(result);
            return Zeze.Transaction.Procedure.Success;
        }

        public override int ProcessCDestroy(CDestroy protocol)
        {
            Login.Session session = Login.Session.Get(protocol);

            GetBag(session.LoginRoleId.Value).Destory(protocol.Argument.Position);
            SChanged result = new SChanged();
            result.Argument.ItemsRemove.Add(protocol.Argument.Position);
            session.SendResponse(result);
            return Zeze.Transaction.Procedure.Success;
        }

        public override int ProcessCSort(CSort protocol)
        {
            Login.Session session = Login.Session.Get(protocol);

            Bag bag = GetBag(session.LoginRoleId.Value);
            bag.Sort();
            SChanged result = new SChanged();
            // 这里直接引用表格中的bean。协议发送完就释放，作为临时变量可以这样用。一般情况下不能保存表格中的bean。
            result.Argument.ItemsReplace.AddRange(bag.Items);
            session.SendResponse(result);

            return Zeze.Transaction.Procedure.Success;
        }

        // for other module
        public Bag GetBag(long roleid)
        {
            return new Bag(roleid, _tbag);
        }
    }
}
