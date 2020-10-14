using System;
using System.Collections.Generic;
using System.Text;

namespace Game.Skill
{
    public class SkillAttack : Skill
    {
        private BSkillAttackExtra extra;

        public SkillAttack(BSkill bean, BSkillAttackExtra extra) : base(bean)
        {
            this.extra = extra;
        }
    }
}
