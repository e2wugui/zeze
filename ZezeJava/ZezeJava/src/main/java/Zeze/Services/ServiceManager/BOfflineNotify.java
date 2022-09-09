package Zeze.Services.ServiceManager;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public class BOfflineNotify extends Bean {
	public int ServerId;
	public String NotifyId;
	public long   NotifySerialId; // context 如果够用就直接用这个，
	public Binary NotifyContext; // context 扩展context。

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteInt(ServerId);
		bb.WriteString(NotifyId);
		bb.WriteLong(NotifySerialId);
		bb.WriteBinary(NotifyContext);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		ServerId = bb.ReadInt();
		NotifyId = bb.ReadString();
		NotifySerialId = bb.ReadLong();
		NotifyContext = bb.ReadBinary();
	}

	@Override
	protected void ResetChildrenRootInfo() {

	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {

	}
}
