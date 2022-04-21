using System;
using System.Collections.Generic;
using System.Text;

namespace Game.Fight
{
    public class Fighter
    {
        public BFighterId Id{ get; }
        public BFighter Bean { get; }

        public Fighter(BFighterId id, BFighter bean)
        {
            this.Id = id;
            this.Bean = bean;
        }

    }
}
