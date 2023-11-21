package Zeze.Services.ServiceManager;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;

public class BOfflineNotify extends Bean {
	public int serverId;
	public String notifyId;
	public long notifySerialId; // context 如果够用就直接用这个，
	public Binary notifyContext = Binary.Empty; // context 扩展context。

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(serverId);
		bb.WriteString(notifyId);
		bb.WriteLong(notifySerialId);
		bb.WriteBinary(notifyContext);
	}

	@Override
	public void decode(IByteBuffer bb) {
		serverId = bb.ReadInt();
		notifyId = bb.ReadString();
		notifySerialId = bb.ReadLong();
		notifyContext = bb.ReadBinary();
	}
}
