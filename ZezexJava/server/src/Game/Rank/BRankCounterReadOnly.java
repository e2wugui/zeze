package Game.Rank;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public interface BRankCounterReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public long getValue();
}