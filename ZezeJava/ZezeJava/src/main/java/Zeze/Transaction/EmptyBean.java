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

		@Override
		protected void initChildrenRootInfoWithRedo(Record.RootInfo root) {
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

	@Override
	public void assign(Zeze.Transaction.Data data) {
	}

	@Override
	public Data toData() {
		return Data.instance;
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

	public static class Data extends Zeze.Transaction.Data {
		// 只用于协议/RPC的不可修改的共享单例,不能放入数据库中
		public static final Data instance = new Data();

		@Override
		public void decode(ByteBuffer bb) {
			bb.SkipUnknownField(ByteBuffer.BEAN);
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteByte(0);
		}

		@Override
		public Data copy() {
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
}
