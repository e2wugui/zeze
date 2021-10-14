package Game.Login;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public interface BReliableNotifyReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public System.Collections.Generic.IReadOnlyList<Zeze.Net.Binary> getNotifies();
	public long getReliableNotifyTotalCountStart();
}