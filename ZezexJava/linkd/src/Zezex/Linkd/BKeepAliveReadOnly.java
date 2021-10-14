package Zezex.Linkd;

import Zeze.Serialize.*;
import Zezex.*;

// auto-generated



public interface BKeepAliveReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public long getTimestamp();
}