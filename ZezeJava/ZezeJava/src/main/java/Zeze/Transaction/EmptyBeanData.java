package Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;

public class EmptyBeanData extends Data {
	// 只用于协议/RPC的不可修改的共享单例,不能放入数据库中
	public static final EmptyBeanData instance = new EmptyBeanData();

	@Override
	public void decode(ByteBuffer bb) {
		bb.SkipUnknownField(ByteBuffer.BEAN);
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteByte(0);
	}

	@Override
	public EmptyBeanData copy() {
		return instance; // data 不可能放入数据库，返回共享的引用是可以的。
	}

	@Override
	public void assign(Bean b) {

	}

	@Override
	public EmptyBean toBean() {
		return new EmptyBean();
	}

	// 必须和EmptyBean.TYPEID一样。
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
