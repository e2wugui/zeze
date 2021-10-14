package Game.Skill;

import Game.*;

public class Skills {
	private long RoleId;
	public final long getRoleId() {
		return RoleId;
	}
	private BSkills Bean;

	public Skills(long roleId, BSkills bean) {
		this.RoleId = roleId;
		this.Bean = bean;
	}

	public final Skill GetSkill(int id) {
		V skill;
		tangible.OutObject<V> tempOut_skill = new tangible.OutObject<V>();
		if (Bean.getSkills().TryGetValue(id, tempOut_skill)) {
		skill = tempOut_skill.outArgValue;
			switch (skill.Extra.TypeId) {
				case BSkillAttackExtra.TYPEID:
					return new SkillAttack(skill, (BSkillAttackExtra)skill.Extra.Bean);
				default:
					throw new RuntimeException("unknown extra");
			}
		}
	else {
		skill = tempOut_skill.outArgValue;
	}
		return null;
	}
}