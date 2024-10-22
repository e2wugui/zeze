
using System.Threading.Tasks;
using Zeze.Arch;
using Zeze.Net;
using Zeze.Transaction;
using Zeze.Transaction.Collections;
using Zeze.Util;

namespace Game.Bag
{
    public sealed partial class ModuleBag : AbstractModule
    {
        public void Start(Game.App app)
        {
            _tBag.ChangeListenerMap.AddListener(new BagChangeListener());
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
            }

            public void OnChanged(object key, Changes.Record changes)
            {
                switch (changes.State)
                {
                    case Changes.Record.Remove:
                        {
                            var changed = new SChanged();
                            changed.Argument.ChangeTag = BChangedResult.ChangeTagRecordIsRemoved;
                            Game.App.Instance.ProviderImplementWithOnline.Online.SendReliableNotify((long)key, Name, changed);
                        }
                        break;

                    case Changes.Record.Put:
                        {
                            var bbag = (BBag)changes.Value;
                            var sbag = new SBag();
                            Bag.ToProtocol(bbag, sbag.Argument);
                            Game.App.Instance.ProviderImplementWithOnline.Online.SendReliableNotify((long)key, Name, sbag);
                        }
                        break;

                    case Changes.Record.Edit:
                        var logbean = changes.GetLogBean();
                        if (logbean.Variables.TryGetValue(tBag.VAR_Items, out var log))
                        {
                            var note = (LogMap2<int, BItem>)log;
                            note.MergeChangedToReplaced();

                            var changed = new SChanged();
                            changed.Argument.ChangeTag = BChangedResult.ChangeTagNormalChanged;
                            changed.Argument.ItemsReplace.AddRange(note.Replaced);
                            foreach (var p in note.Removed)
                                changed.Argument.ItemsRemove.Add(p);

                            Game.App.Instance.ProviderImplementWithOnline.Online.SendReliableNotify((long)key, Name, changed);
                        }
                        break;
                }
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
            session.SendResponseWhileCommit(rpc);
            return 0;
        }

        protected override async Task<long> ProcessDestroyRequest(Protocol p)
        {
            var rpc = p as Destroy;
            var session = ProviderUserSession.Get(rpc);
            var moduleCode = (await GetBag(session.RoleId.Value)).Destroy(rpc.Argument.Position);
            if (0 != moduleCode)
                return ErrorCode((ushort)moduleCode);
            session.SendResponseWhileCommit(rpc);
            return 0;
        }

        protected override async Task<long> ProcessSortRequest(Protocol p)
        {
            var rpc = p as Sort;
            var session = ProviderUserSession.Get(rpc);
            Bag bag = await GetBag(session.RoleId.Value);
            bag.Sort();
            session.SendResponseWhileCommit(rpc);
            return ResultCode.Success;
        }

        protected override async Task<long> ProcessGetBagRequest(Protocol p)
        {
            var rpc = p as GetBag;
            var session = ProviderUserSession.Get(rpc);

            (await GetBag(session.RoleId.Value)).ToProtocol(rpc.Result);
            session.SendResponseWhileCommit(rpc);
            await Game.App.Instance.ProviderImplementWithOnline.Online.AddReliableNotifyMark(
                session.RoleId.Value, BagChangeListener.Name);
            return ResultCode.Success;
        }

        // for other module
        public async Task<Bag> GetBag(long roleid)
        {
            return new Bag(roleid, await _tBag.GetOrAddAsync(roleid));
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
                    return ResultCode.Success;
            }
            return ResultCode.LogicError;

        }
    }
}
