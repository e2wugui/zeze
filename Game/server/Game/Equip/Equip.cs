using System;
using System.Collections.Generic;
using System.Text;

namespace Game.Equip
{
    public class Equip : Game.Item.Item
    {

        public Equip(int position, Game.Bag.BItem bItem, BEquipExtra extra) : base(position, bItem)
        {

        }

        public override bool Use()
        {
            return false;
        }
    }
}
