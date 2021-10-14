package Game.Rank;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public interface BRankListResultReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public int getRankType();
	public System.Collections.Generic.IReadOnlyList<Game.Rank.BRankValueReadOnly> getRankList();
}