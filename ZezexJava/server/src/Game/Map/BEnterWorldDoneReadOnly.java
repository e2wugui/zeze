package Game.Map;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public interface BEnterWorldDoneReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public int getMapInstanceId();
}