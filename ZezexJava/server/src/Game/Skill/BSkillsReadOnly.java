package Game.Skill;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public interface BSkillsReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	 public System.Collections.Generic.IReadOnlyDictionary<Integer,Game.Skill.BSkillReadOnly> getSkills();
}