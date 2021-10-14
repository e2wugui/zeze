package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

// auto-generated



public interface BSendReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public System.Collections.Generic.IReadOnlySet<Long> getLinkSids();
	public int getProtocolType();
	public Zeze.Net.Binary getProtocolWholeData();
	public long getConfirmSerialId();
}