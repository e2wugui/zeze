package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

// auto-generated



public interface BLinkBrokenReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public String getAccount();
	public long getLinkSid();
	public int getReason();
	public System.Collections.Generic.IReadOnlyList<Long> getStates();
	public Zeze.Net.Binary getStatex();
}