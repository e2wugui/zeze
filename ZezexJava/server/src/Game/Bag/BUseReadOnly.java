package Game.Bag;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public interface BUseReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public int getPosition();
}