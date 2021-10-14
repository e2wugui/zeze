package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

// auto-generated



public interface BModuleRedirectAllRequestReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public int getModuleId();
	public int getHashCodeConcurrentLevel();
	public System.Collections.Generic.IReadOnlySet<Integer> getHashCodes();
	public long getSourceProvider();
	public long getSessionId();
	public String getMethodFullName();
	public Zeze.Net.Binary getParams();
	public String getServiceNamePrefix();
}