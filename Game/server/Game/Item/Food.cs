using System;
using System.Collections.Generic;
using System.Text;

namespace Game.Item
{
    public class Food : Item
    {
        private BFoodExtra extra;

        public Food(ContainerWithOne container, BFoodExtra extra) : base(container)
        {
            this.extra = extra;
        }

        public int Account => extra.Ammount;

        public override void Use()
        {
            throw new NotImplementedException();
        }
    }
}
