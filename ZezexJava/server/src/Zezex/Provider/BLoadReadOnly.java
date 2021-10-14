package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

// auto-generated



public interface BLoadReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public int getOnline();
	public int getProposeMaxOnline();
	public int getOnlineNew();
}