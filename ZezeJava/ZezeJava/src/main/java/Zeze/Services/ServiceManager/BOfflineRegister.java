package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public class BOfflineRegister extends Bean {
	public int ServerId;
	public String NotifyId;

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteInt(ServerId);
		bb.WriteString(NotifyId);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		ServerId = bb.ReadInt();
		NotifyId = bb.ReadString();
	}

	@Override
	protected void ResetChildrenRootInfo() {

	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {

	}
}
