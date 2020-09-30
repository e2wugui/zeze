using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Transaction;

namespace Game.Bag
{
    public class ContainerOne : Game.Item.ContainerWithOne
    {
        private Bag bag;
        private int position;
        private BItem bItem;

        public ContainerOne(Bag bag, int position, BItem bItem)
        {
            this.bag = bag;
            this.position = position;
            this.bItem = bItem;
        }
        public bool Remove(int number)
        {
            return bag.Remove(position, bItem.Id, number);
        }
        public int ItemId => bItem.Id;

        public Item.Item ToItem()
        {
            Zeze.Transaction.Bean dynamicBean = bItem.Extra;
            switch (dynamicBean.TypeId)
            {
                case Item.BFoodExtra.TYPEID: return new Item.Food(this, (Item.BFoodExtra)dynamicBean);
                case Item.BHorseExtra.TYPEID: return new Item.Horse(this, (Item.BHorseExtra)dynamicBean);
                case Equip.BEquipExtra.TYPEID: return new Equip.Equip(this, (Equip.BEquipExtra)dynamicBean);
                default:
                    throw new System.Exception("unknown extra");
            }
        }
    }
}
