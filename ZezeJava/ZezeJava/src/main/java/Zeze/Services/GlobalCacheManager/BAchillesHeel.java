package Zeze.Services.GlobalCacheManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public class BAchillesHeel extends Bean {
	public int ServerId; // 必须的。

	public String SecureKey; // 安全验证
	public int GlobalCacheManagerHashIndex; // 安全验证

	@Override
	public void decode(ByteBuffer bb) {
		ServerId = bb.ReadInt();
		SecureKey = bb.ReadString();
		GlobalCacheManagerHashIndex = bb.ReadInt();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(ServerId);
		bb.WriteString(SecureKey);
		bb.WriteInt(GlobalCacheManagerHashIndex);
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void resetChildrenRootInfo() {
		throw new UnsupportedOperationException();
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

	@Override
	public String toString() {
		return "BAchillesHeel{" + "ServerId=" + ServerId + ", SecureKey='" + SecureKey + '\'' +
				", GlobalCacheManagerHashIndex=" + GlobalCacheManagerHashIndex + '}';
	}
}
