package Game.Login;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public interface BOnlineReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public String getLinkName();
	public long getLinkSid();
	public int getState();
	public System.Collections.Generic.IReadOnlySet<String> getReliableNotifyMark();
	public System.Collections.Generic.IReadOnlyList<Zeze.Net.Binary> getReliableNotifyQueue();
	public long getReliableNotifyConfirmCount();
	public long getReliableNotifyTotalCount();
	public int getProviderId();
	public long getProviderSessionId();
}