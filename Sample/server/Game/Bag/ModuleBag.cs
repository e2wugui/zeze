
using System.Threading.Tasks;
using Zeze.Arch;
using Zeze.Net;
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

                Game.App.Instance.ProviderImplementWithOnline.Online.SendReliableNotify((long)key, Name, sbag);
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
                Game.App.Instance.ProviderImplementWithOnline.Online.SendReliableNotify((long)key, Name, changed);
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

                Game.App.Instance.ProviderImplementWithOnline.Online.SendReliableNotify((long)key, Name, changed);
            }

            void ChangeListener.OnRemoved(object key)
            {
                // 整个记录删除，由 BagChangeListener 处理。
            }
        }

        // protocol handles
        protected override async Task<long> ProcessMoveRequest(Protocol p)
        {
            var rpc = p as Move;
            var session = ProviderUserSession.Get(rpc);
            // throw exception if not login
            var moduleCode = (await GetBag(session.RoleId.Value)).Move(
                rpc.Argument.PositionFrom,
                rpc.Argument.PositionTo,
                rpc.Argument.Number);
            if (moduleCode != 0)
                return ErrorCode((ushort)moduleCode);
            session.SendResponse(rpc);
            return 0;
        }

        protected override async Task<long> ProcessDestroyRequest(Protocol p)
        {
            var rpc = p as Destroy;
            var session = ProviderUserSession.Get(rpc);
            var moduleCode = (await GetBag(session.RoleId.Value)).Destroy(rpc.Argument.Position);
            if (0 != moduleCode)
                return ErrorCode((ushort)moduleCode);
            session.SendResponse(rpc);
            return 0;
        }

        protected override async Task<long> ProcessSortRequest(Protocol p)
        {
            var rpc = p as Sort;
            var session = ProviderUserSession.Get(rpc);
            Bag bag = await GetBag(session.RoleId.Value);
            bag.Sort();
            session.SendResponse(rpc);
            return Procedure.Success;
        }

        protected override async Task<long> ProcessGetBagRequest(Protocol p)
        {
            var rpc = p as GetBag;
            var session = ProviderUserSession.Get(rpc);

            (await GetBag(session.RoleId.Value)).ToProtocol(rpc.Result);
            session.SendResponse(rpc);
            await Game.App.Instance.ProviderImplementWithOnline.Online.AddReliableNotifyMark(
                session.RoleId.Value, BagChangeListener.Name);
            return Procedure.Success;
        }

        // for other module
        public async Task<Bag> GetBag(long roleid)
        {
            return new Bag(roleid, await _tbag.GetOrAddAsync(roleid));
        }

        protected override async Task<long> ProcessCUse(Protocol p)
        {
            var protocol = p as CUse;
            var session = ProviderUserSession.Get(protocol);
            Bag bag = await GetBag(session.RoleId.Value);
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
