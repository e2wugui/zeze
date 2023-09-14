package Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
	public void decode(@NotNull ByteBuffer bb) {
		bb.SkipUnknownField(ByteBuffer.BEAN);
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteByte(0);
	}

	@Override
	public @NotNull EmptyBean copy() {
		return new EmptyBean();
	}

	@Override
	public void assign(Zeze.Transaction.Data data) {
	}

	@Override
	public @NotNull Data toData() {
		return Data.instance;
	}

	public static final long TYPEID = 0; // 用0，而不是Bean.Hash("")，可能0更好吧。

	@Override
	public long typeId() {
		return TYPEID;
	}

	@Override
	public @NotNull String toString() {
		return "()";
	}

	@Override
	public int preAllocSize() {
		return 1;
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return obj != null && getClass() == obj.getClass();
	}

	public static class Data extends Zeze.Transaction.Data {
		public static final Data instance = new Data();

		// 没有状态,所以可以安全共享使用instance,不公开构造了
		private Data() {
		}

		@Override
		public void decode(@NotNull ByteBuffer bb) {
			bb.SkipUnknownField(ByteBuffer.BEAN);
		}

		@Override
		public void encode(@NotNull ByteBuffer bb) {
			bb.WriteByte(0);
		}

		@Override
		public void reset() {
		}

		@Override
		public @NotNull Data copy() {
			return this; // EmptyBean.Data没有任何状态,返回相同实例是安全的
		}

		@Override
		public void assign(Bean b) {
		}

		@Override
		public @NotNull EmptyBean toBean() {
			return new EmptyBean();
		}

		// 必须和EmptyBean.TYPEID一样。
		public static final long TYPEID = 0; // 用0，而不是Bean.Hash("")，可能0更好吧。

		@Override
		public long typeId() {
			return TYPEID;
		}

		@Override
		public @NotNull String toString() {
			return "()";
		}

		@Override
		public int preAllocSize() {
			return 1;
		}

		@Override
		public int hashCode() {
			return 0;
		}

		@Override
		public boolean equals(@Nullable Object obj) {
			return obj != null && getClass() == obj.getClass();
		}
	}
}
