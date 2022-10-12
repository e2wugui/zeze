package Zeze.Transaction;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Util.KV;
import Zeze.Util.Str;

public abstract class Bean implements Serializable {
	public static final int OBJECT_ID_STEP = 4096; // 自增长步长。低位保留给Variable.Id。也就是，Variable.Id 最大只能是4095.
	public static final int MAX_VARIABLE_ID = OBJECT_ID_STEP - 1;
	private static final AtomicLong objectIdGen = new AtomicLong();

	// 这个方法应该仅用于内部。
	@Deprecated
	public static long nextObjectId() {
		return objectIdGen.addAndGet(OBJECT_ID_STEP);
	}

	private transient final long objectId = nextObjectId();

	protected transient Record.RootInfo rootInfo;

	// Parent VariableId 是 ChangeListener 需要的属性。
	// Parent 和 TableKey 一起初始化，仅在被Table管理以后才设置。
	private transient Bean parent;

	// VariableId 初始化分两部分：
	// 1. Bean 包含的 Bean 在构造的时候初始化，同时初始化容器的LogKey（包含 VariableId）
	// 2. Bean 加入容器时，由容器初始化。使用容器所在Bean的LogKey中的VariableId初始化。
	private transient int variableId;

	public Bean() {
	}

	public Bean(int variableId) {
		this.variableId = variableId;
	}

	public final long objectId() {
		return objectId;
	}

	public final TableKey tableKey() {
		return rootInfo == null ? null : rootInfo.getTableKey();
	}

	public final Bean parent() {
		return parent;
	}

	public final int variableId() {
		return variableId;
	}

	// 这个方法应该仅用于内部。
	@Deprecated
	public final void variableId(int value) {
		variableId = value;
	}

	/**
	 * 构建 ChangeListener 链。其中第一个KeyValuePair在调用前加入，这个由Log或者ChangeNote提供。
	 *
	 * @param path path
	 */
	public final void buildChangeListenerPath(ArrayList<KV<Bean, Integer>> path) {
		for (Bean parent = this.parent; parent != null; parent = parent.parent)
			path.add(KV.create(parent, variableId));
	}

	public final boolean isManaged() {
		return rootInfo != null;
	}

	public final void initRootInfoWithRedo(Record.RootInfo rootInfo, Bean parent) {
		initRootInfo(rootInfo, parent);
		Transaction.whileRedo(this::resetRootInfo);
	}

	public final void initRootInfo(Record.RootInfo rootInfo, Bean parent) {
		if (isManaged())
			throw new HasManagedException();
		this.rootInfo = rootInfo;
		this.parent = parent;
		initChildrenRootInfo(rootInfo);
	}

	public void resetRootInfo() {
		rootInfo = null;
		parent = null;
		resetChildrenRootInfo();
	}

	protected abstract void resetChildrenRootInfo();

	// 用在第一次加载Bean时，需要初始化它的root
	protected abstract void initChildrenRootInfo(Record.RootInfo root);

	public boolean negativeCheck() {
		return false;
	}

	public Bean copy() {
		throw new UnsupportedOperationException();
	}

	public void buildString(StringBuilder sb, int level) {
		sb.append(Str.indent(level)).append('{').append(this).append('}');
	}

	// Bean的类型Id，替换 ClassName，提高效率和存储空间
	// 用来支持 dynamic 类型，或者以后的扩展。
	// 默认实现是 ClassName.HashCode()，也可以手动指定一个值。
	// Gen的时候会全局判断是否出现重复冲突。如果出现冲突，则手动指定一个。
	// 这个方法在Gen的时候总是覆盖(override)，提供默认实现是为了方便内部Bean的实现。
	public long typeId() {
		return hash64(getClass().getName());
	}

	// 使用自己的hash算法，因为 TypeId 会持久化，不能因为算法改变导致值变化。
	// XXX: 这个算法定好之后，就不能变了。
	public static long hash64(String name) {
		// This is a Knuth hash
		long hashedValue = 3074457345618258791L;
		for (int i = 0; i < name.length(); i++) {
			hashedValue += name.charAt(i);
			hashedValue *= 3074457345618258799L;
		}
		return hashedValue;
	}

	public static int hash32(String name) {
		long hash64 = hash64(name);
		return (int)(hash64 ^ (hash64 >> 32));
	}

	public Object mapKey() {
		throw new UnsupportedOperationException();
	}

	public void mapKey(Object mapKey) {
		throw new UnsupportedOperationException();
	}

	public void followerApply(Log log) {
		throw new UnsupportedOperationException();
	}

	public LogBean createLogBean() {
		var log = new LogBean();
		log.setBelong(parent);
		log.setThis(this);
		log.setVariableId(variableId);
		return log;
	}

	// 当Bean放到Table中时，用来支持数据版本号。
	public long version() {
		return 0;
	}

	// package use in Transaction.finalCommit
	protected void version(long newVersion) {
		// 子类实现
	}
}
