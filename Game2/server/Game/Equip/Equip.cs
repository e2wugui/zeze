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

        public override void CalculateFighter(Game.Fight.Fighter fighter)
        {
            fighter.Bean.Attack += 20.0f;
            fighter.Bean.Defence += 20.0f;
        }

        public override bool Use()
        {
            return false;
        }
    }
}
