using System;
using System.Collections.Generic;
using System.Text;
using Game.Buf;

namespace Game.Buf
{
    public class BufExtra : Buf
    {
        private BBufExtra extra;

        public BufExtra(BBuf bean, BBufExtra extra) : base(bean)
        {
            this.extra = extra;
        }

        public override void CalculateFighter(Game.Fight.Fighter fighter)
        {
            fighter.Bean.Attack += 10.0f;
            fighter.Bean.Defence += 10.0f;
        }
    }
}
