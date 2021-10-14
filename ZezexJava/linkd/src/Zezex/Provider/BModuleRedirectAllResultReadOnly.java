package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

// auto-generated



public interface BModuleRedirectAllResultReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public int getModuleId();
	public int getServerId();
	public long getSourceProvider();
	public String getMethodFullName();
	public long getSessionId();
	 public System.Collections.Generic.IReadOnlyDictionary<Integer,Zezex.Provider.BModuleRedirectAllHashReadOnly> getHashs();
}