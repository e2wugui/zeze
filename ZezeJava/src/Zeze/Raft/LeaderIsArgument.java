package Zeze.Raft;

import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.*;
import java.util.*;

/** 
 下面是非标准的Raft-Rpc，辅助Agent用的。
*/
public final class LeaderIsArgument extends Bean {
	private long Term;
	public long getTerm() {
		return Term;
	}
	public void setTerm(long value) {
		Term = value;
	}
	private String LeaderId;
	public String getLeaderId() {
		return LeaderId;
	}
	public void setLeaderId(String value) {
		LeaderId = value;
	}

	@Override
	public void Decode(ByteBuffer bb) {
		setTerm(bb.ReadLong());
		setLeaderId(bb.ReadString());
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteLong(getTerm());
		bb.WriteString(getLeaderId());
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return String.format("(Term=%1$s LeaderId=%2$s)", getTerm(), getLeaderId());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		boolean tempVar = obj instanceof LeaderIsArgument;
		LeaderIsArgument other = tempVar ? (LeaderIsArgument)obj : null;
		if (tempVar) {
			return getTerm() == other.getTerm() && getLeaderId().equals(other.getLeaderId());
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int _prime_ = 31;
		int _h_ = 0;
		_h_ = _h_ * _prime_ + (new Long(getTerm())).hashCode();
		_h_ = _h_ * _prime_ + getLeaderId().hashCode();
		return _h_;
	}
}