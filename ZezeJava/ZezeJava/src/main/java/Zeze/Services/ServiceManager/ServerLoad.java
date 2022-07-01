package Zeze.Services.ServiceManager;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public final class ServerLoad extends Bean {
	public String Ip;
	public int Port;
	public Binary Param = Binary.Empty;

	public String getName() {
		return Ip + ":" + Port;
	}

	@Override
	public void Decode(ByteBuffer bb) {
		Ip = bb.ReadString();
		Port = bb.ReadInt();
		Param = bb.ReadBinary();
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteString(Ip);
		bb.WriteInt(Port);
		bb.WriteBinary(Param);
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	private static int _PRE_ALLOC_SIZE_ = 16;

	@Override
	public int getPreAllocSize() {
		return _PRE_ALLOC_SIZE_;
	}

	@Override
	public void setPreAllocSize(int size) {
		_PRE_ALLOC_SIZE_ = size;
	}

	@Override
	public String toString() {
		return "ServerLoad{" + "Ip='" + Ip + '\'' + ", Port=" + Port + ", Param=" + Param + '}';
	}
}
