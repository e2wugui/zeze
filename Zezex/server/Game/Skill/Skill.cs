using System;
using System.Collections.Generic;
using System.Text;

namespace Game.Skill
{
    public class Skill
    {
        private BSkill Bean;

        public Skill(BSkill bean)
        {
            this.Bean = bean;
        }

        public virtual void Perform(Game.Fight.Fighter op, Game.Fight.Fighter target)
        {

        }
    }
}
