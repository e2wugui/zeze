package Game.Rank;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public interface BRankValueReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public long getRoleId();
	public long getValue();
	public Zeze.Net.Binary getValueEx();
	public boolean getAwardTaken();
}