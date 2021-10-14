package Game.Buf;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public interface BBufChangedReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	 public System.Collections.Generic.IReadOnlyDictionary<Integer,Game.Buf.BBufReadOnly> getReplace();
	public System.Collections.Generic.IReadOnlySet<Integer> getRemove();
	public int getChangeTag();
}