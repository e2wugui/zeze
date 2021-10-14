package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

// auto-generated



public interface BModuleRedirectResultReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public int getModuleId();
	public int getServerId();
	public int getReturnCode();
	public Zeze.Net.Binary getParams();
	public System.Collections.Generic.IReadOnlyList<Zezex.Provider.BActionParamReadOnly> getActions();
}