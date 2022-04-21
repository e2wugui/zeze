using System;
using System.Collections.Generic;
using System.Text;

namespace Game.Skill
{
    public class Skills
    {
        public long RoleId { get; }
        private BSkills Bean;

        public Skills(long roleId, BSkills bean)
        {
            this.RoleId = roleId;
            this.Bean = bean;
        }

        public Skill GetSkill(int id)
        {
            if (Bean.Skills.TryGetValue(id, out var skill))
            {
                switch (skill.Extra.TypeId)
                {
                    case BSkillAttackExtra.TYPEID: return new SkillAttack(skill, (BSkillAttackExtra)skill.Extra.Bean);
                    default:
                        throw new System.Exception("unknown extra");
                }
            }
            return null;
        }
    }
}
