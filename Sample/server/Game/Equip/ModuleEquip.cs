
using Game.Fight;
using System.Threading.Tasks;
using Zeze.Arch;
using Zeze.Net;
using Zeze.Transaction;
using Zeze.Transaction.Collections;
using Zeze.Util;

namespace Game.Equip
{
    public sealed partial class ModuleEquip : AbstractModule
    {
        public void Start(Game.App app)
        {
            _tEquip.ChangeListenerMap.AddListener(new ItemsChangeListener());
        }

        public void Stop(Game.App app)
        {
        }

        class ItemsChangeListener : ChangeListener
        {
            public static string Name { get; } = "Game.Equip.Items";

            public void OnChanged(object key, Changes.Record changes)
            {
                switch (changes.State)
                {
                    case Changes.Record.Remove:
                        {
                            var changed = new SEquipement();
                            changed.Argument.ChangeTag = Game.Bag.BChangedResult.ChangeTagRecordIsRemoved;
                            Game.App.Instance.ProviderImplementWithOnline.Online.SendReliableNotify((long)key, Name, changed);
                        }
                        break;

                    case Changes.Record.Put:
                        {
                            // 记录改变，通知全部。
                            var bequips = (BEquips)changes.Value;

                            var changed = new SEquipement();
                            changed.Argument.ChangeTag = Game.Bag.BChangedResult.ChangeTagRecordChanged;
                            changed.Argument.ItemsReplace.AddRange(bequips.Items);

                            Game.App.Instance.ProviderImplementWithOnline.Online.SendReliableNotify((long)key, Name, changed);
                        }
                        break;

                    case Changes.Record.Edit:
                        {
                            var logbean = changes.GetLogBean();
                            if (logbean.Variables.TryGetValue(tEquip.VAR_Items, out var note))
                            {
                                // 增量变化，通知变更。
                                var notemap2 = (LogMap2<int, Game.Bag.BItem>)note;
                                notemap2.MergeChangedToReplaced();

                                var changed = new SEquipement();
                                changed.Argument.ChangeTag = Game.Bag.BChangedResult.ChangeTagNormalChanged;

                                changed.Argument.ItemsReplace.AddRange(notemap2.Replaced);
                                foreach (var p in notemap2.Removed)
                                    changed.Argument.ItemsRemove.Add(p);

                                Game.App.Instance.ProviderImplementWithOnline.Online.SendReliableNotify((long)key, Name, changed);
                            }
                        }
                        break;
                }
            }
        }

        public int GetEquipPosition(int itemId)
        {
            return 0;
            // 如果装备可以穿到多个位置，则需要选择其中的一个位置返回。
            // 比如戒指，优先返回空的位置，都不为空（可能的规则）返回等级低的位置。
            // 如果物品不能装备到身上的话，返回错误(-1).
            //return -1;
        }
        // 装备只有装上取下两个操作，没有公开的需求，先不提供包装类了。

        protected override async Task<long> ProcessEquipementRequest(Protocol p)
        {
            var rpc = p as Equipement;
            var session = ProviderUserSession.Get(rpc);

            Bag.Bag bag = await App.Instance.Game_Bag.GetBag(session.RoleId.Value);
            if (bag.Items.TryGetValue(rpc.Argument.BagPos, out var bItem))
            {
                int equipPos = GetEquipPosition(bItem.Id);
                if (equipPos < 0)
                    return ErrorCode(ResultCodeCannotEquip);

                BEquips equips = await _tEquip.GetOrAddAsync(session.RoleId.Value);
                Game.Bag.BItem bEquipAdd;
                if (equips.Items.TryGetValue(equipPos, out var eItem))
                {
                    // 装备目标位置已经存在装备，交换。
                    // 先都删除，这样就能在原位置上交换的装备，否则对于包裹可能加到其他位置。
                    equips.Items.Remove(equipPos);
                    bag.Remove(rpc.Argument.BagPos, bItem.Id, 1);

                    bag.Add(rpc.Argument.BagPos, new Bag.BItem() {
                        Id = eItem.Id, Number = 1, Extra_Game_Equip_BEquipExtra = eItem.Extra_Game_Equip_BEquipExtra.Copy() }
                        );

                    bEquipAdd = new Game.Bag.BItem() { Id = bItem.Id, Number = 1, Extra_Game_Equip_BEquipExtra = bItem.Extra_Game_Equip_BEquipExtra.Copy() };
                    equips.Items.Add(equipPos, bEquipAdd);
                }
                else
                {
                    // 装备目标位置为空
                    bag.Remove(rpc.Argument.BagPos, bItem.Id, 1);
                    bEquipAdd = new Game.Bag.BItem() { Id = bItem.Id, Number = 1, Extra_Game_Equip_BEquipExtra = bItem.Extra_Game_Equip_BEquipExtra.Copy() };
                    equips.Items.Add(equipPos, bEquipAdd);
                }
                session.SendResponseWhileCommit(rpc);
                return ResultCode.Success;
            }
            return ErrorCode(ResultCodeItemNotFound);
        }

        protected override async Task<long> ProcessUnequipementRequest(Protocol p)
        {
            var rpc = p as Unequipement;
            var session = ProviderUserSession.Get(rpc);

            BEquips equips = await _tEquip.GetOrAddAsync(session.RoleId.Value);
            if (equips.Items.TryGetValue(rpc.Argument.EquipPos, out var eItem))
            {
                equips.Items.Remove(rpc.Argument.EquipPos);
                var bag = await App.Instance.Game_Bag.GetBag(session.RoleId.Value);
                var bItemAdd = new Bag.BItem() { Id = eItem.Id, Number = 1, Extra_Game_Equip_BEquipExtra = (BEquipExtra)eItem.Extra.Bean.Copy() };
                if (0 != bag.Add(-1, bItemAdd))
                    return ErrorCode(ResultCodeBagIsFull); // bag is full
                session.SendResponseWhileCommit(rpc);
                return ResultCode.Success;
            }

            return ErrorCode(ResultCodeEquipNotFound);
        }

        public async Task<Game.Item.Item> GetEquipItem(long roleId, int position)
        {
            BEquips equips = await _tEquip.GetOrAddAsync(roleId);
            return GetEquipItem(equips, position);
        }

        public Game.Item.Item GetEquipItem(BEquips equips, int position)
        {
            if (equips.Items.TryGetValue(position, out var equip))
            {
                switch (equip.Extra.TypeId)
                {
                    case BEquipExtra.TYPEID: return new Equip(equip, (BEquipExtra)equip.Extra.Bean);
                    default:
                        throw new System.Exception("unknown extra");
                }

            }
            return null;
        }

        public async Task CalculateFighter(Game.Fight.Fighter fighter)
        {
            if (fighter.Id.Type != BFighterId.TypeRole)
                return;

            BEquips equips = await _tEquip.GetOrAddAsync(fighter.Id.InstanceId);
            foreach (var pos in equips.Items.Keys)
            {
                GetEquipItem(equips, pos).CalculateFighter(fighter);
            }
        }
    }
}
