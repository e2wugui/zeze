
using Zeze.Transaction;

namespace Game.Bag
{
    public sealed partial class ModuleBag : AbstractModule
    {
        public void Start(Game.App app)
        {
            _tbag.ChangeListenerMap.AddListener(tbag.VAR_Items, new ItemsChangeListener());
        }

        public void Stop(Game.App app)
        {
        }

        class ItemsChangeListener : Zeze.Transaction.ChangeListener
        {
            public static string Name { get; } = "Game.Bag.Items";

            void ChangeListener.OnChanged(object key, Bean value)
            {
                // 记录改变，通知全部。
                BBag bbag = (BBag)value;
                var bag = new SGetBag();
                Bag.ToProtocol(bbag, bag);

                Game.App.Instance.Game_Login.Onlines.SendReliableNotify((long)key, Name, bag);
            }

            void ChangeListener.OnChanged(object key, Bean value, ChangeNote note)
            {
                // 增量变化，通知变更。
                ChangeNoteMap2<int, BItem> notemap2 = (ChangeNoteMap2<int, BItem>)note;
                BBag bbag = (BBag)value;
                notemap2.MergeChangedToReplaced(bbag.Items);

                SChanged changed = new SChanged();
                changed.Argument.ChangeTag = BChangedResult.ChangeTagNormalChanged;

                changed.Argument.ItemsReplace.AddRange(notemap2.Replaced);
                foreach (var p in notemap2.Removed)
                    changed.Argument.ItemsRemove.Add(p);

                Game.App.Instance.Game_Login.Onlines.SendReliableNotify((long)key, Name, changed);
            }

            void ChangeListener.OnRemoved(object key)
            {
                SChanged changed = new SChanged();
                changed.Argument.ChangeTag = BChangedResult.ChangeTagRecordIsRemoved;
                Game.App.Instance.Game_Login.Onlines.SendReliableNotify((long)key, Name, changed);
            }
        }
        // protocol handles
        public override int ProcessCMove(CMove protocol)
        {
            Login.Session session = Login.Session.Get(protocol);
            // throw exception if not login
            GetBag(session.RoleId.Value).Move(protocol.Argument.PositionFrom, protocol.Argument.PositionTo, protocol.Argument.Number);
            return Zeze.Transaction.Procedure.Success;
        }

        public override int ProcessCDestroy(CDestroy protocol)
        {
            Login.Session session = Login.Session.Get(protocol);
            GetBag(session.RoleId.Value).Destory(protocol.Argument.Position);
            return Zeze.Transaction.Procedure.Success;
        }

        public override int ProcessCSort(CSort protocol)
        {
            Login.Session session = Login.Session.Get(protocol);
            Bag bag = GetBag(session.RoleId.Value);
            bag.Sort();
            return Zeze.Transaction.Procedure.Success;
        }

        public override int ProcessCGetBag(CGetBag protocol)
        {
            Login.Session session = Login.Session.Get(protocol);

            SGetBag result = new SGetBag();
            GetBag(session.RoleId.Value).ToProtocol(result);
            session.SendResponse(result);
            Game.App.Instance.Game_Login.Onlines.AddReliableNotifyMark(session.RoleId.Value, ItemsChangeListener.Name);
            return Zeze.Transaction.Procedure.Success;
        }

        // for other module
        public Bag GetBag(long roleid)
        {
            return new Bag(roleid, _tbag.GetOrAdd(roleid));
        }

        public override int ProcessCUse(CUse protocol)
        {
            Login.Session session = Login.Session.Get(protocol);
            Bag bag = GetBag(session.RoleId.Value);
            Item.Item item = bag.GetItem(protocol.Argument.Position);
            if (null != item && item.Use())
            {
                if (bag.Remove(protocol.Argument.Position, item.Id, 1))
                    return Procedure.Success;
            }
            return Procedure.LogicError;

        }
    }
}
