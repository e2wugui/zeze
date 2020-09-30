using System;
using System.Collections.Generic;
using System.Text;

namespace Game.Item
{
    public class Horse : Item
    {
        private BHorseExtra extra;

        public Horse(ContainerWithOne container, BHorseExtra extra) : base(container)
        {
            this.extra = extra;
        }

        public int Speed => extra.Speed;

        public override void Use()
        {
            
        }
    }
}
