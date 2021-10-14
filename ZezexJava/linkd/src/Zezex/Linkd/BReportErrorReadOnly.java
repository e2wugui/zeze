package Zezex.Linkd;

import Zeze.Serialize.*;
import Zezex.*;

// auto-generated



public interface BReportErrorReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public int getFrom();
	public int getCode();
	public String getDesc();
}