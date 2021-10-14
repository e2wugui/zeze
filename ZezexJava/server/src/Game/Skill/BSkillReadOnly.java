package Game.Skill;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public interface BSkillReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public int getId();
	public Zeze.Transaction.DynamicBeanReadOnly getExtra();

	public Game.Skill.BSkillAttackExtraReadOnly getExtraGameSkillBSkillAttackExtra();
}