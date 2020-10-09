using System;
using System.Collections.Generic;
using System.Text;

namespace Game.Item
{
    public class Food : Item
    {
        private BFoodExtra extra;

        public Food(int position, Game.Bag.BItem bItem, BFoodExtra extra) : base(position, bItem)
        {
            this.extra = extra;
        }

        public int Account => extra.Ammount;

        public override bool Use()
        {
            throw new NotImplementedException();
        }
    }
}
