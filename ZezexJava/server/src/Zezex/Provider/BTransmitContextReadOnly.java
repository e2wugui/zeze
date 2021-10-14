package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

// auto-generated



public interface BTransmitContextReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public long getLinkSid();
	public int getProviderId();
	public long getProviderSessionId();
}