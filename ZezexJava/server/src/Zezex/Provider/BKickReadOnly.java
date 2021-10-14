package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

// auto-generated



public interface BKickReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public long getLinksid();
	public int getCode();
	public String getDesc();
}