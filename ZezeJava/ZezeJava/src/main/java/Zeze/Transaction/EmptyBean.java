package Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;

public class EmptyBean extends Bean {
	// 只用于协议/RPC的不可修改的共享单例,不能放入数据库中
	public static final EmptyBean instance = new EmptyBean() {
		@Override
		protected void initChildrenRootInfo(Record.RootInfo root) {
			resetRootInfo();
			throw new UnsupportedOperationException();
		}
	};

	@Override
	public void decode(ByteBuffer bb) {
		bb.SkipUnknownField(ByteBuffer.BEAN);
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteByte(0);
	}

	@Override
	public EmptyBean copy() {
		return new EmptyBean();
	}

	public static final long TYPEID = 0; // 用0，而不是Bean.Hash("")，可能0更好吧。

	@Override
	public long typeId() {
		return TYPEID;
	}

	@Override
	public String toString() {
		return "()";
	}

	@Override
	public int preAllocSize() {
		return 1;
	}
}
