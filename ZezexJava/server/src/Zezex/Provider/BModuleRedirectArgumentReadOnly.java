package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

// auto-generated



public interface BModuleRedirectArgumentReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public int getModuleId();
	public int getHashCode();
	public String getMethodFullName();
	public Zeze.Net.Binary getParams();
	public String getServiceNamePrefix();
}