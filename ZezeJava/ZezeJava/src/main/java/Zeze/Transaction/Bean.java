package Zeze.Transaction;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Util.KV;
import Zeze.Util.Str;
import org.apache.commons.lang3.NotImplementedException;

public abstract class Bean implements Serializable {
	public static final int ObjectIdStep = 4096; // 自增长步长。低位保留给Variable.Id。也就是，Variable.Id 最大只能是4095.
	public static final int MaxVariableId = ObjectIdStep - 1;

	private static final AtomicLong _objectIdGen = new AtomicLong();

	public static long getNextObjectId() {
		return _objectIdGen.addAndGet(ObjectIdStep);
	}

	private final long ObjectId = getNextObjectId();

	protected Record.RootInfo RootInfo;

	// Parent VariableId 是 ChangeListener 需要的属性。
	// Parent 和 TableKey 一起初始化，仅在被Table管理以后才设置。
	private Bean Parent;

	// VariableId 初始化分两部分：
	// 1. Bean 包含的 Bean 在构造的时候初始化，同时初始化容器的LogKey（包含 VariableId）
	// 2. Bean 加入容器时，由容器初始化。使用容器所在Bean的LogKey中的VariableId初始化。
	public int VariableId;

	public Bean() {
	}

	public Bean(int variableId) {
		VariableId = variableId;
	}

	public final long getObjectId() {
		return ObjectId;
	}

	public final TableKey getTableKey() {
		return RootInfo == null ? null : RootInfo.getTableKey();
	}

	public final Bean getParent() {
		return Parent;
	}

	private void setParent(Bean value) {
		Parent = value;
	}

	public final int getVariableId() {
		return VariableId;
	}

	// 这个方法应该仅用于内部。
	public final void setVariableId(int value) {
		VariableId = value;
	}

	/**
	 * 构建 ChangeListener 链。其中第一个KeyValuePair在调用前加入，这个由Log或者ChangeNote提供。
	 *
	 * @param path path
	 */
	public final void BuildChangeListenerPath(ArrayList<KV<Bean, Integer>> path) {
		for (Bean parent = Parent; parent != null; parent = parent.Parent)
			path.add(KV.Create(parent, VariableId));
	}

	public final boolean isManaged() {
		return RootInfo != null;
	}

	public final void InitRootInfo(Record.RootInfo rootInfo, Bean parent) {
		if (isManaged())
			throw new HasManagedException();
		RootInfo = rootInfo;
		setParent(parent);
		InitChildrenRootInfo(rootInfo);
	}

	// 用在第一次加载Bean时，需要初始化它的root
	protected abstract void InitChildrenRootInfo(Record.RootInfo root);

	public boolean NegativeCheck() {
		return false;
	}

	public Bean CopyBean() {
		throw new UnsupportedOperationException();
	}

	public void BuildString(StringBuilder sb, int level) {
		sb.append(Str.indent(level)).append('{').append(this).append('}');
	}

	// Bean的类型Id，替换 ClassName，提高效率和存储空间
	// 用来支持 dynamic 类型，或者以后的扩展。
	// 默认实现是 ClassName.HashCode()，也可以手动指定一个值。
	// Gen的时候会全局判断是否出现重复冲突。如果出现冲突，则手动指定一个。
	// 这个方法在Gen的时候总是覆盖(override)，提供默认实现是为了方便内部Bean的实现。
	public long getTypeId() {
		return Hash64(getClass().getName());
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
		return hashedValue;
	}

	public static int Hash32(String name) {
		long hash64 = Hash64(name);
		return (int)(hash64 ^ (hash64 >> 32));
	}

	public Object getMapKey() {
		throw new UnsupportedOperationException();
	}

	public void setMapKey(Object mapKey) {
		throw new UnsupportedOperationException();
	}

	public void FollowerApply(Log log) {
		throw new NotImplementedException("");
	}

	public LogBean CreateLogBean() {
		throw new NotImplementedException("");
	}
}
