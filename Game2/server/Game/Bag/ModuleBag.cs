
using Zeze.Transaction;

namespace Game.Bag
{
    public sealed partial class ModuleBag : AbstractModule
    {
        public void Start(Game.App app)
        {
            _tbag.ChangeListenerMap.AddListener(tbag.VAR_Items, new ItemsChangeListener());
            _tbag.ChangeListenerMap.AddListener(tbag.VAR_All, new BagChangeListener());
        }

        public void Stop(Game.App app)
        {
        }

        class BagChangeListener : ChangeListener
        {
            public static string Name { get; } = "Game.Bag";

            public void OnChanged(object key, Bean value)
            {
                // 记录改变，通知全部。
                BBag bbag = (BBag)value;
                var sbag = new SBag();
                Bag.ToProtocol(bbag, sbag.Argument);

                Game.App.Instance.Game_Login.Onlines.SendReliableNotify((long)key, Name, sbag);
            }

            public void OnChanged(object key, Bean value, ChangeNote note)
            {
                // 整个记录改变没有 note，只有Map,Set才有note。
                OnChanged(key, value);
            }

            public void OnRemoved(object key)
            {
                SChanged changed = new SChanged();
                changed.Argument.ChangeTag = BChangedResult.ChangeTagRecordIsRemoved;
                Game.App.Instance.Game_Login.Onlines.SendReliableNotify((long)key, Name, changed);
            }
        }

        class ItemsChangeListener : ChangeListener
        {
            public string Name => BagChangeListener.Name;

            void ChangeListener.OnChanged(object key, Bean value)
            {
                // 整个记录改变，由 BagChangeListener 处理。发送包含 Money, Capacity.
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
                // 整个记录删除，由 BagChangeListener 处理。
            }
        }

        // protocol handles
        public override int ProcessMoveRequest(Move rpc)
        {
            Login.Session session = Login.Session.Get(rpc);
            // throw exception if not login
            var moduleCode = GetBag(session.RoleId.Value).Move(
                rpc.Argument.PositionFrom,
                rpc.Argument.PositionTo,
                rpc.Argument.Number);
            if (moduleCode != 0)
                return ReturnCode((ushort)moduleCode);
            session.SendResponse(rpc);
            return 0;
        }

        public override int ProcessDestroyRequest(Destroy rpc)
        {
            Login.Session session = Login.Session.Get(rpc);
            var moduleCode = GetBag(session.RoleId.Value).Destory(rpc.Argument.Position);
            if (0 != moduleCode)
                return ReturnCode((ushort)moduleCode);
            session.SendResponse(rpc);
            return 0;
        }

        public override int ProcessSortRequest(Sort rpc)
        {
            Login.Session session = Login.Session.Get(rpc);
            Bag bag = GetBag(session.RoleId.Value);
            bag.Sort();
            session.SendResponse(rpc);
            return Procedure.Success;
        }

        public override int ProcessGetBagRequest(GetBag rpc)
        {
            Login.Session session = Login.Session.Get(rpc);

            GetBag(session.RoleId.Value).ToProtocol(rpc.Result);
            session.SendResponse(rpc);
            Game.App.Instance.Game_Login.Onlines.AddReliableNotifyMark(
                session.RoleId.Value, BagChangeListener.Name);
            return Procedure.Success;
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
