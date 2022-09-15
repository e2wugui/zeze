package Zeze.Services.GlobalCacheManager;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public class BGlobalKeyState extends Bean {
	public Binary GlobalKey; // 没有初始化，使用时注意
	public int State;

	@Override
	public void decode(ByteBuffer bb) {
		GlobalKey = bb.ReadBinary();
		State = bb.ReadInt();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteBinary(GlobalKey);
		bb.WriteInt(State);
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void resetChildrenRootInfo() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return GlobalKey + ":" + State;
	}

	private static int _PRE_ALLOC_SIZE_ = 16;

	@Override
	public int preAllocSize() {
		return _PRE_ALLOC_SIZE_;
	}

	@Override
	public void preAllocSize(int size) {
		_PRE_ALLOC_SIZE_ = size;
	}
}
