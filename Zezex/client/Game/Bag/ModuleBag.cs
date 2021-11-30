
using Zeze.Net;

namespace Game.Bag
{
    public sealed partial class ModuleBag : AbstractModule, Login.IReliableNotify
    {
        public void Start(Game.App app)
        {
            Game.App.Instance.Game_Login.RegisterReliableNotify(SChanged.TypeId_, this);
            Game.App.Instance.Game_Login.RegisterReliableNotify(SBag.TypeId_, this); // 记录整个变更用这个通告
        }

        public void Stop(Game.App app)
        {
        }

        public void OnReliableNotify(Zeze.Net.Protocol p)
        {
            switch (p.TypeId)
            {
                case SChanged.TypeId_:
                    ProcessSChanged((SChanged)p);
                    break;
                case SBag.TypeId_:
                    ProcessSBag(p as SBag);
                    break;
            }
        }

        protected override long ProcessSChanged(Protocol p)
        {
            var protocol = p as SChanged;
            switch (protocol.Argument.ChangeTag)
            {
                case BChangedResult.ChangeTagRecordChanged:
                    // 记录改变还需要更新money,capacity。但是listener只监听了items。
                    // server 在发现整个记录变更时，发送了SBag。不会发这个改变。see server::Game.Bag.Module。
                    /*
                    bag.Items.Clear();
                    bag.Items.SetItems(protocol.Argument.ItemsReplace);
                    */
                    break;
                case BChangedResult.ChangeTagRecordIsRemoved:
                    bag = null;
                    break;
                case BChangedResult.ChangeTagNormalChanged:
                    bag.Items.SetItems(protocol.Argument.ItemsReplace);
                    foreach (var r in protocol.Argument.ItemsRemove)
                        bag.Items.Remove(r);
                    break;
            }
            return Zeze.Transaction.Procedure.Success;
        }

        private BBag bag;

        protected override long ProcessSBag(Protocol p)
        {
            var protocol = p as SBag;
            bag = protocol.Argument;
            return 0;
        }
    }
}
