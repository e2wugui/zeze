package Zeze.Raft;

import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.*;
import java.util.*;

/** 
 LeaderIs 的发送时机
 0. Agent 刚连上来时，如果Node是当前Leader，它马上这个Rpc给Agent。
 1. Node 收到应用请求时，发现自己不是Leader，发送重定向。此时Node不处理请求（也不返回结果）。
 2. 选举结束时也给Agent广播选举结果。
*/
public final class LeaderIs extends Rpc<LeaderIsArgument, EmptyBean> {
	public final static int ProtocolId_ = Bean.Hash16(LeaderIs.class.FullName);

	@Override
	public int getModuleId() {
		return 0;
	}
	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}
}