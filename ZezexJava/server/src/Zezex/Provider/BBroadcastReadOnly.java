package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

// auto-generated



public interface BBroadcastReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public int getProtocolType();
	public Zeze.Net.Binary getProtocolWholeData();
	public int getTime();
	public long getConfirmSerialId();
}