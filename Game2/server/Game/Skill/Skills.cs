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
                Zeze.Transaction.Bean dynamicBean = skill.Extra;
                switch (dynamicBean.TypeId)
                {
                    case BSkillAttackExtra.TYPEID: return new SkillAttack(skill, (BSkillAttackExtra)dynamicBean);
                    default:
                        throw new System.Exception("unknown extra");
                }
            }
            return null;
        }
    }
}
