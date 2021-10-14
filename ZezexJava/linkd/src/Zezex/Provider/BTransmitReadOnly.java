package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

// auto-generated



public interface BTransmitReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public String getActionName();
	 public System.Collections.Generic.IReadOnlyDictionary<Long,Zezex.Provider.BTransmitContextReadOnly> getRoles();
	public long getSender();
	public String getServiceNamePrefix();
}