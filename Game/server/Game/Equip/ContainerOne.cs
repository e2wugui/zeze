using System;
using System.Collections.Generic;
using System.Text;

namespace Game.Equip
{
    public class ContainerOne : Item.ContainerWithOne
    {
        private BEquip equip;

        public int ItemId => equip.Id;

        public bool Remove(int number)
        {
            throw new NotImplementedException();
        }

        public ContainerOne(BEquip equip)
        {
            this.equip = equip;
        }

        public Item.Item ToItem()
        {
            Zeze.Transaction.Bean dynamicBean = equip.Extra;
            switch (dynamicBean.TypeId)
            {
                case BEquipExtra.TYPEID: return new Equip(this, (BEquipExtra)dynamicBean);
                default:
                    throw new System.Exception("unknown extra");
            }
        }
    }
}
