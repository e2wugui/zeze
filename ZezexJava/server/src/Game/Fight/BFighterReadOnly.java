package Game.Fight;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public interface BFighterReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public float getAttack();
	public float getDefence();
}