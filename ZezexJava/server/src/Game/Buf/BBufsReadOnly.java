package Game.Buf;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public interface BBufsReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	 public System.Collections.Generic.IReadOnlyDictionary<Integer,Game.Buf.BBufReadOnly> getBufs();
}