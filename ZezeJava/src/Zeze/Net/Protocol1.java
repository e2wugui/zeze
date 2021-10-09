package Zeze.Net;

import Zeze.Serialize.*;
import Zeze.*;

//C# TO JAVA CONVERTER TODO TASK: The C# 'new()' constraint has no equivalent in Java:
//ORIGINAL LINE: public abstract class Protocol<TArgument> : Protocol where TArgument : Zeze.Transaction.Bean, new()
public abstract class Protocol<TArgument extends Zeze.Transaction.Bean> extends Protocol {
	private TArgument Argument = new TArgument();
	public final TArgument getArgument() {
		return Argument;
	}
	public final void setArgument(TArgument value) {
		Argument = value;
	}

	@Override
	public void Decode(ByteBuffer bb) {
		setResultCode(bb.ReadInt());
		setUniqueRequestId(bb.ReadLong());
		getArgument().Decode(bb);
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteInt(getResultCode());
		bb.WriteLong(getUniqueRequestId());
		getArgument().Encode(bb);
	}

	@Override
	public String toString() {
		return String.format("%1$s UniqueRequestId=%2$s ResultCode=%3$s%4$s\tArgument=%5$s", this.getClass().getName(), getUniqueRequestId(), getResultCode(), System.lineSeparator(), getArgument());
	}
}