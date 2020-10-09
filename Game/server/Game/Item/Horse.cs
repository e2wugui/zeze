using System;
using System.Collections.Generic;
using System.Text;

namespace Game.Item
{
    public class Horse : Item
    {
        private BHorseExtra extra;

        public Horse(Game.Bag.BItem bItem, BHorseExtra extra) : base(bItem)
        {
            this.extra = extra;
        }

        public int Speed => extra.Speed;

        public override bool Use()
        {
            return false;
        }
    }
}
