package Zeze.Services.ServiceManager;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public class BOfflineRegister extends Bean {
	public int ServerId;
	public String NotifyId;
	public Binary NotifyContext;

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteInt(ServerId);
		bb.WriteString(NotifyId);
		bb.WriteBinary(NotifyContext);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		ServerId = bb.ReadInt();
		NotifyId = bb.ReadString();
		NotifyContext = bb.ReadBinary();
	}

	@Override
	protected void ResetChildrenRootInfo() {

	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {

	}
}
