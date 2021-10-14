package Game.Skill;

import Game.*;

public class SkillAttack extends Skill {
	private BSkillAttackExtra extra;

	public SkillAttack(BSkill bean, BSkillAttackExtra extra) {
		super(bean);
		this.extra = extra;
	}
}