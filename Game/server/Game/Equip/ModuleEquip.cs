
using Game.Fight;
using Zeze.Transaction;

namespace Game.Equip
{
    public sealed partial class ModuleEquip : AbstractModule
    {
        public void Start(Game.App app)
        {
            _tequip.ChangeListenerMap.AddListener(tequip.VAR_Items, new ItemsChangeListener());
        }

        public void Stop(Game.App app)
        {
        }

        class ItemsChangeListener : Zeze.Transaction.ChangeListener
        {
            void ChangeListener.OnChanged(object key, Bean value)
            {
                // 记录改变，通知全部。
                BEquips bequips = (BEquips)value;

                SEquipement changed = new SEquipement();
                changed.Argument.ChangeTag = Game.Bag.BChangedResult.ChangeTagRecordChanged;
                changed.Argument.ItemsReplace.AddRange(bequips.Items);

                Game.App.Instance.Game_Login.Onlines.Send((long)key, changed);
            }

            void ChangeListener.OnChanged(object key, Bean value, ChangeNote note)
            {
                // 增量变化，通知变更。
                ChangeNoteMap2<int, Game.Bag.BItem> notemap2 = (ChangeNoteMap2<int, Game.Bag.BItem>)note;
                BEquips bequips = (BEquips)value;
                notemap2.MergeChangedToReplaced(bequips.Items);

                SEquipement changed = new SEquipement();
                changed.Argument.ChangeTag = Game.Bag.BChangedResult.ChangeTagNormalChanged;

                changed.Argument.ItemsReplace.AddRange(notemap2.Replaced);
                foreach (var p in notemap2.Removed)
                    changed.Argument.ItemsRemove.Add(p);

                Game.App.Instance.Game_Login.Onlines.Send((long)key, changed);
            }

            void ChangeListener.OnRemoved(object key)
            {
                SEquipement changed = new SEquipement();
                changed.Argument.ChangeTag = Game.Bag.BChangedResult.ChangeTagRecordIsRemoved;
                Game.App.Instance.Game_Login.Onlines.Send((long)key, changed);
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

        public override int ProcessCEquipement(CEquipement protocol)
        {
            Login.Session session = Login.Session.Get(protocol);

            Bag.Bag bag = App.Instance.Game_Bag.GetBag(session.LoginRoleId.Value);
            if (bag.Items.TryGetValue(protocol.Argument.BagPos, out var bItem))
            {
                int equipPos = GetEquipPosition(bItem.Id);
                if (equipPos < 0)
                    return Zeze.Transaction.Procedure.LogicError;

                BEquips equips = _tequip.GetOrAdd(session.LoginRoleId.Value);
                Game.Bag.BItem bEquipAdd;
                if (equips.Items.TryGetValue(equipPos, out var eItem))
                {
                    // 装备目标位置已经存在装备，交换。
                    // 先都删除，这样就能在原位置上交换的装备，否则对于包裹可能加到其他位置。
                    equips.Items.Remove(equipPos);
                    bag.Remove(protocol.Argument.BagPos, bItem.Id, 1);

                    bag.Add(protocol.Argument.BagPos, new Bag.BItem() {
                        Id = eItem.Id, Number = 1, Extra_Game_Equip_BEquipExtra = eItem.Extra_Game_Equip_BEquipExtra.Copy() }
                        );

                    bEquipAdd = new Game.Bag.BItem() { Id = bItem.Id, Number = 1, Extra_Game_Equip_BEquipExtra = bItem.Extra_Game_Equip_BEquipExtra.Copy() };
                    equips.Items.Add(equipPos, bEquipAdd);
                }
                else
                {
                    // 装备目标位置为空
                    bag.Remove(protocol.Argument.BagPos, bItem.Id, 1);
                    bEquipAdd = new Game.Bag.BItem() { Id = bItem.Id, Number = 1, Extra_Game_Equip_BEquipExtra = bItem.Extra_Game_Equip_BEquipExtra.Copy() };
                    equips.Items.Add(equipPos, bEquipAdd);
                }
                return Zeze.Transaction.Procedure.Success;
            }
            return Zeze.Transaction.Procedure.LogicError;
        }

        public override int ProcessCUnequipement(CUnequipement protocol)
        {
            Login.Session session = Login.Session.Get(protocol);

            BEquips equips = _tequip.GetOrAdd(session.LoginRoleId.Value);
            if (equips.Items.TryGetValue(protocol.Argument.EquipPos, out var eItem))
            {
                equips.Items.Remove(protocol.Argument.EquipPos);
                Bag.Bag bag = App.Instance.Game_Bag.GetBag(session.LoginRoleId.Value);
                Bag.BItem bItemAdd = new Bag.BItem() { Id = eItem.Id, Number = 1, Extra_Game_Equip_BEquipExtra = (BEquipExtra)eItem.Extra.CopyBean() };
                if (0 != bag.Add(-1, bItemAdd))
                    return Zeze.Transaction.Procedure.LogicError; // bag is full
                return Zeze.Transaction.Procedure.Success;
            }

            return Zeze.Transaction.Procedure.LogicError;
        }

        public Game.Item.Item GetEquipItem(long roleId, int position)
        {
            BEquips equips = _tequip.GetOrAdd(roleId);
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

        public void CalculateFighter(Game.Fight.Fighter fighter)
        {
            if (fighter.Id.Type != BFighterId.TypeRole)
                return;

            BEquips equips = _tequip.GetOrAdd(fighter.Id.InstanceId);
            foreach (var pos in equips.Items.Keys)
            {
                GetEquipItem(equips, pos).CalculateFighter(fighter);
            }
        }
    }
}
