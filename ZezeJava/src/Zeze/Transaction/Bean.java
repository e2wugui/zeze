package Zeze.Transaction;

import java.util.*;

public abstract class Bean implements Zeze.Serialize.Serializable {
	private static java.util.concurrent.atomic.AtomicLong _objectIdGen = new java.util.concurrent.atomic.AtomicLong();

	public static final int ObjectIdStep = 4096; // 自增长步长。低位保留给Variable.Id。也就是，Variable.Id 最大只能是4095.
	public static final int MaxVariableId = ObjectIdStep - 1;

	public static long getNextObjectId() {
		return _objectIdGen.addAndGet(ObjectIdStep);
	}

	private long ObjectId = getNextObjectId();
	public final long getObjectId() {
		return ObjectId;
	}

	protected Zeze.Transaction.Record.RootInfo RootInfo;

	public final TableKey getTableKey() {
		return RootInfo == null ? null : RootInfo.getTableKey();
	}

	// Parent VariableId 是 ChangeListener 需要的属性。
	// Parent 和 TableKey 一起初始化，仅在被Table管理以后才设置。
	private Bean Parent;
	public final Bean getParent() {
		return Parent;
	}
	private void setParent(Bean value) {
		Parent = value;
	}
	// VariableId 初始化分两部分：
	// 1. Bean 包含的 Bean 在构造的时候初始化，同时初始化容器的LogKey（包含 VariableId）
	// 2. Bean 加入容器时，由容器初始化。使用容器所在Bean的LogKey中的VariableId初始化。
	private int VariableId;
	public final int getVariableId() {
		return VariableId;
	}

	// TODO 这个方法应该仅用于内部。
	public final void setVariableId(int value) {
		VariableId = value;
	}

	public Bean() {
	}

	public Bean(int variableId) {
		this.setVariableId(variableId);
	}

	/** 
	 构建 ChangeListener 链。其中第一个KeyValuePair在调用前加入，这个由Log或者ChangeNote提供。
	 
	 @param path
	 @return 
	*/
	public final void BuildChangeListenerPath(ArrayList<Zeze.Util.KV<Bean, Integer>> path) {
		for (Bean parent = getParent(); parent != null; parent = parent.Parent) {
			path.add(Zeze.Util.KV.Create(parent, getVariableId()));
		}
	}

	public final boolean isManaged() {
		return RootInfo != null;
	}

	public final void InitRootInfo(Record.RootInfo rootInfo, Bean parent) {
		if (isManaged()) {
			throw new HasManagedException();
		}
		this.RootInfo = rootInfo;
		this.setParent(parent);
		InitChildrenRootInfo(rootInfo);
	}

	// 用在第一次加载Bean时，需要初始化它的root
	protected abstract void InitChildrenRootInfo(Record.RootInfo root);

	public abstract void Decode(Zeze.Serialize.ByteBuffer bb);
	public abstract void Encode(Zeze.Serialize.ByteBuffer bb);

	// helper
	public int getCapacityHintOfByteBuffer() {
		return 1024;
	}
	public boolean NegativeCheck() {
		return false;
	}
	public Bean CopyBean() {
		throw new UnsupportedOperationException();
	}
	public void BuildString(StringBuilder sb, int level) {
		sb.append(" ".repeat(level)).append("{}").append(System.lineSeparator());
	}

	// Bean的类型Id，替换 ClassName，提高效率和存储空间
	// 用来支持 dynamic 类型，或者以后的扩展。
	// 默认实现是 ClassName.HashCode()，也可以手动指定一个值。
	// Gen的时候会全局判断是否出现重复冲突。如果出现冲突，则手动指定一个。
	// 这个方法在Gen的时候总是覆盖(override)，提供默认实现是为了方便内部Bean的实现。
	public long getTypeId() {
		return Hash64(this.getClass().getName());
	}

	// 使用自己的hash算法，因为 TypeId 会持久化，不能因为算法改变导致值变化。
	// XXX: 这个算法定好之后，就不能变了。
	public static long Hash64(String name) {
		// This is a Knuth hash
		long hashedValue = 3074457345618258791L;
		for (int i = 0; i < name.length(); i++) {
			hashedValue += name.charAt(i);
			hashedValue *= 3074457345618258799L;
		}
		return (long)hashedValue;
	}

	public static int Hash32(String name) {
		long hash64 = (long)Hash64(name);
		int hash32 = (int)(hash64 & 0xffffffff) ^ (int)(hash64 >>> 32);
		return hash32;
	}

	public static short Hash16(String protocolName) {
		long hash64 = (long)Hash64(protocolName);
		int hash32 = (int)(hash64 & 0xffffffff) ^ (int)(hash64 >>> 32);
		short hash16 = (short)((hash32 & 0xffff) ^ (hash32 >>> 16));
		return hash16;
	}
}