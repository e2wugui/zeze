using System;
using System.Collections.Generic;
using System.Text;

namespace Game.Item
{
    public class Food : Item
    {
        private BFoodExtra extra;

        public Food(Game.Bag.BItem bItem, BFoodExtra extra) : base(bItem)
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
