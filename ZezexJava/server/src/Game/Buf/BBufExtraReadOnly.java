package Game.Buf;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public interface BBufExtraReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

}