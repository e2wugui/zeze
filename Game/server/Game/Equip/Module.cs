

namespace Game.Equip
{
    public sealed partial class Module : AbstractModule
    {
        public void Start(Game.App app)
        {
        }

        public void Stop(Game.App app)
        {
        }

        // 装备只有装上取下两个操作，没有公开的需求，先不提供包装类了。

        public override int ProcessCEquipement(CEquipement protocol)
        {
            Login.Session session = Login.Session.Get(protocol);

            SEquipement result = new SEquipement();

            Bag.Bag bag = App.Instance.Game_Bag_Module.GetBag(session.LoginRoleId.Value);
            if (bag.Items.TryGetValue(protocol.Argument.BagPos, out var bItem))
            {
                int equipPos = 0; // TODO GetEquipPosition(bItem.Id); // 不是装备的化，应该返回错误的position.
                BEquips equips = _tequip.GetOrAdd(session.LoginRoleId.Value);
                BEquip bEquipAdd;
                if (equips.Items.TryGetValue(equipPos, out var eItem))
                {
                    // 装备目标位置已经存在装备，交换。
                    // 先都删除，这样就能在原位置上交换的装备，否则对于包裹可能加到其他位置。
                    equips.Items.Remove(equipPos);
                    bag.Remove(protocol.Argument.BagPos, bItem.Id, 1);

                    Bag.BItem bItemAdd = new Bag.BItem() { Id = eItem.Id, Number = 1 };
                    bItemAdd.ExtraSet((BEquipExtra)eItem.Extra.CopyBean()); // 方法1
                    bag.Add(protocol.Argument.BagPos, bItemAdd, result.Argument.BagChanged);

                    bEquipAdd = new BEquip() { Id = bItem.Id };
                    bEquipAdd.ExtraSet(((BEquipExtra)bItem.Extra).Copy()); // 方法2
                    equips.Items.Add(equipPos, bEquipAdd);
                    result.Argument.EquipReplaced.Add(equipPos, bEquipAdd);
                }
                else
                {
                    // 装备目标位置为空
                    bag.Remove(protocol.Argument.BagPos, bItem.Id, 1);
                    bEquipAdd = new BEquip() { Id = bItem.Id };
                    bEquipAdd.ExtraSet((BEquipExtra)bItem.Extra.CopyBean());
                    equips.Items.Add(equipPos, bEquipAdd);
                    result.Argument.EquipReplaced.Add(equipPos, bEquipAdd);
                }
                session.SendResponse(result);
                return Zeze.Transaction.Procedure.Success;
            }
            return Zeze.Transaction.Procedure.LogicError;
        }

        public override int ProcessCUnequipement(CUnequipement protocol)
        {
            Login.Session session = Login.Session.Get(protocol);

            SEquipement result = new SEquipement();
            BEquips equips = _tequip.GetOrAdd(session.LoginRoleId.Value);
            if (equips.Items.TryGetValue(protocol.Argument.EquipPos, out var eItem))
            {
                equips.Items.Remove(protocol.Argument.EquipPos);
                Bag.Bag bag = App.Instance.Game_Bag_Module.GetBag(session.LoginRoleId.Value);
                Bag.BItem bItemAdd = new Bag.BItem() { Id = eItem.Id, Number = 1 };
                bItemAdd.ExtraSet((BEquipExtra)eItem.Extra.CopyBean());
                if (0 != bag.Add(-1, bItemAdd, result.Argument.BagChanged))
                    return Zeze.Transaction.Procedure.LogicError; // bag is full
                result.Argument.EquipRemoved = protocol.Argument.EquipPos;
                session.SendResponse(result);
                return Zeze.Transaction.Procedure.Success;
            }

            return Zeze.Transaction.Procedure.LogicError;
        }

        public ContainerOne GetContainerOne(long roleId, int position)
        {
            BEquips equips = _tequip.GetOrAdd(roleId);
            if (equips.Items.TryGetValue(position, out var equip))
                return new ContainerOne(equip);
            throw new System.NullReferenceException(); // XXX 找不到物品返回null?
        }
    }
}
