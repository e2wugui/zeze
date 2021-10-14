package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

// auto-generated



public interface BBindReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	 public System.Collections.Generic.IReadOnlyDictionary<Integer,Zezex.Provider.BModuleReadOnly> getModules();
	public System.Collections.Generic.IReadOnlySet<Long> getLinkSids();
}