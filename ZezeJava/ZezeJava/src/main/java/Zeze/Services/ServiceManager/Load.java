package Zeze.Services.ServiceManager;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;
import Zeze.Util.Func1;

public final class Load extends Bean {
	public String Ip;
	public int Port;
	public Binary Param = Binary.Empty;

	public volatile Object ObjectParam; // Decoded Param. 不会系列化。

	public <T> T get(Func1<Binary, T> decoder) {
		if (ObjectParam == null) {
			try {
				// decoder 必须允许重复执行，这里没有保证仅调用一次。
				ObjectParam = decoder.call(Param);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
		return (T)ObjectParam;
	}

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
}
