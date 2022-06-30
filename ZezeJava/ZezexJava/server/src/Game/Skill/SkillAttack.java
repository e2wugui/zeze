package Game.Skill;

public class SkillAttack extends Skill {
	public final BSkillAttackExtra extra;

	public SkillAttack(BSkill bean, BSkillAttackExtra extra) {
		super(bean);
		this.extra = extra;
	}
}
