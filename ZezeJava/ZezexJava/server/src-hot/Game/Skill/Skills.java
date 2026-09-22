package Game.Skill;

public class Skills {
	private final long RoleId;
	public final long getRoleId() {
		return RoleId;
	}
	private final BSkills Bean;

	public Skills(long roleId, BSkills bean) {
		this.RoleId = roleId;
		this.Bean = bean;
	}

	public final Skill GetSkill(int id) {
		var skill = Bean.getSkills().get(id);
		if (null != skill) {
			var extraTypeId = skill.getExtra().getTypeId();
			if (extraTypeId == BSkillAttackExtra.TYPEID)
				return new SkillAttack(skill, (BSkillAttackExtra)skill.getExtra().getBean());
			throw new RuntimeException("unknown extra");
		}
		return null;
	}
}
