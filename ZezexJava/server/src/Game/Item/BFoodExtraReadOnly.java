package Game.Item;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public interface BFoodExtraReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public int getAmmount();
}