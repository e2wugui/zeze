package Game.Bag;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public interface BChangedResultReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	 public System.Collections.Generic.IReadOnlyDictionary<Integer,Game.Bag.BItemReadOnly> getItemsReplace();
	public System.Collections.Generic.IReadOnlySet<Integer> getItemsRemove();
	public int getChangeTag();
}