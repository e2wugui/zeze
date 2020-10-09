using System;
using System.Collections.Generic;
using System.Text;

namespace Game.Equip
{
    public class Equip : Game.Item.Item
    {

        public Equip(Game.Bag.BItem bItem, BEquipExtra extra) : base(bItem)
        {

        }

        public override bool Use()
        {
            return false;
        }
    }
}
