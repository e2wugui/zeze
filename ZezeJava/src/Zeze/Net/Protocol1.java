package Zeze.Net;

import Zeze.Serialize.*;

public abstract class Protocol1<TArgument extends Zeze.Transaction.Bean> extends Protocol {
	public TArgument Argument;

	@Override
	public void Decode(ByteBuffer bb) {
		setResultCode(bb.ReadInt());
		setUniqueRequestId(bb.ReadLong());
		Argument.Decode(bb);
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteInt(getResultCode());
		bb.WriteLong(getUniqueRequestId());
		Argument.Encode(bb);
	}

	@Override
	public String toString() {
		return Zeze.Util.Str.format("{} UniqueRequestId={} ResultCode={}{}\tArgument={}", this.getClass().getName(), getUniqueRequestId(), getResultCode(), System.lineSeparator(), Argument);
	}
}