package Game.Login;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public interface BReliableNotifyConfirmReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public long getReliableNotifyConfirmCount();
}