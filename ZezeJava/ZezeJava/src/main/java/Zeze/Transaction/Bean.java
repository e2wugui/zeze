package Zeze.Transaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import Zeze.Builtin.HotDistribute.BVariable;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Util.KV;
import Zeze.Util.Reflect;
import Zeze.Util.Str;
import org.jspecify.annotations.Nullable;
import Zeze.Transaction.Record.RootInfo;

/**
 * 受管数据 Bean 基类：承载 objectId、rootInfo/parent 受管关系与修改日志（Log），
 * 是表格记录与事务化集合元素的数据基元。
 */
public abstract class Bean implements Serializable {
	public static final int OBJECT_ID_STEP = 4096; // 自增长步长。低位保留给Variable.Id。也就是，Variable.Id 最大只能是4095.
	public static final int MAX_VARIABLE_ID = OBJECT_ID_STEP - 1;
	private static final AtomicLong objectIdGen = new AtomicLong();

	private transient final long objectId = objectIdGen.addAndGet(OBJECT_ID_STEP);

	protected transient @Nullable RootInfo rootInfo;

	// Parent VariableId 是 ChangeListener 需要的属性。
	// Parent 和 TableKey 一起初始化，仅在被Table管理以后才设置。
	private transient @Nullable Bean parent;

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

	public final @Nullable TableKey tableKey() {
		return rootInfo != null ? rootInfo.tableKey() : null;
	}

	public final @Nullable Bean parent() {
		return parent;
	}

	public final int variableId() {
		return variableId;
	}

	@Deprecated // 这个方法应该仅用于内部。
	public final void variableId(int value) {
		variableId = value;
	}

	/**
	 * 构建 ChangeListener 链。其中第一个KeyValuePair在调用前加入，这个由Log或者ChangeNote提供。
	 * 每条路径元素为（父Bean, 当前层子Bean的variableId）：第1层是this在自己parent中的编号，
	 * 更深层各取该层子Bean自己的编号（不得恒用this.variableId，层级≥2会取错）。
	 *
	 * @param path path
	 */
	public final void buildChangeListenerPath(ArrayList<KV<Bean, Integer>> path) {
		for (Bean child = this, parent = this.parent; parent != null; child = parent, parent = parent.parent)
			path.add(KV.create(parent, child.variableId));
	}

	public final boolean isManaged() {
		return rootInfo != null;
	}

	// 【bean所有权语义】bean加入受管容器（表记录/受管集合）即被事务占有（rootInfo在此登记，
	// 经Transaction.whileRedo进redoBeans）：redo重试由triggerRedoActions统一resetRootInfo
	// 后随重放重新登记；而最终回滚不解除占有（redoBeans仅在redo路径重置，终局回滚不触碰），
	// redo-only是既定设计。复用被占有的bean（再次加入受管容器）将抛HasManagedException
	// （下方入口检查），请重建实例或copy()。
	public final void initRootInfoWithRedo(@Nullable RootInfo rootInfo, @Nullable Bean parent) {
		if (isManaged())
			Objects.requireNonNull(Transaction.getCurrent()).throwAbort("hasManaged", new HasManagedException());
		this.rootInfo = rootInfo;
		this.parent = parent;
		Transaction.whileRedo(this);
		if (rootInfo != null) // followerApply 等非受管场景 rootInfo 为 null，子 bean 保持非受管即可
			initChildrenRootInfoWithRedo(rootInfo);
	}

	public final void initRootInfo(@Nullable RootInfo rootInfo, @Nullable Bean parent) {
		if (isManaged())
			Objects.requireNonNull(Transaction.getCurrent()).throwAbort("hasManaged", new HasManagedException());
		this.rootInfo = rootInfo;
		this.parent = parent;
		if (rootInfo != null) // 同上
			initChildrenRootInfo(rootInfo);
	}

	public final void resetRootInfo() {
		rootInfo = null;
		parent = null;
	}

	// 用在第一次加载Bean时，需要初始化它的root
	protected void initChildrenRootInfo(RootInfo root) {
	}

	protected void initChildrenRootInfoWithRedo(RootInfo root) {
	}

	public boolean negativeCheck() {
		return false;
	}

	// 重置Bean子类的所有字段
	public void reset() {
	}

	public Bean copy() {
		throw new UnsupportedOperationException(getClass().getName());
	}

	public void assign(Data data) {
		throw new UnsupportedOperationException(getClass().getName());
	}

	public Data toData() {
		throw new UnsupportedOperationException(getClass().getName());
	}

	@SuppressWarnings("unchecked")
	public static <B extends Bean, D extends Data> void toBeanList(Collection<D> fromDataList,
																   Collection<B> toBeanList) {
		for (var data : fromDataList)
			toBeanList.add((B)data.toBean());
	}

	@SuppressWarnings("unchecked")
	public static <B extends Bean, D extends Data> void toDataList(Collection<B> fromBeanList,
																   Collection<D> toDataList) {
		for (var bean : fromBeanList)
			toDataList.add((D)bean.toData());
	}

	@SuppressWarnings("unchecked")
	public static <K, B extends Bean, D extends Data> void toBeanMap(Map<K, D> fromDataMap,
																	 Map<K, B> toBeanMap) {
		for (var e : fromDataMap.entrySet())
			toBeanMap.put(e.getKey(), (B)e.getValue().toBean());
	}

	@SuppressWarnings("unchecked")
	public static <K, B extends Bean, D extends Data> void toDataMap(Map<K, B> fromBeanMap,
																	 Map<K, D> toDataMap) {
		for (var e : fromBeanMap.entrySet())
			toDataMap.put(e.getKey(), (D)e.getValue().toData());
	}

	public void buildString(StringBuilder sb, int level) {
		sb.append(Str.indent(level)).append('{').append(this).append('}');
	}

	// Bean的类型Id，替换 ClassName，提高效率和存储空间
	// 用来支持 dynamic 类型，或者以后的扩展。
	// 默认实现是 ClassName.HashCode()，也可以手动指定一个值。
	// Gen的时候会全局判断是否出现重复冲突。如果出现冲突，则手动指定一个。
	// 这个方法在Gen的时候总是覆盖(override)，提供默认实现是为了方便内部Bean的实现。
	@Override
	public long typeId() {
		return hash64(getClass().getName());
	}

	// 使用自己的hash算法，因为 TypeId 会持久化，不能因为算法改变导致值变化。
	// 算法已固化进 TypeId，不得变更，改了即数据不兼容。
	public static long hash64(long initial, String name, int n) {
		// This is a Knuth hash
		long hashedValue = initial;
		for (int i = 0; i < n; i++)
			hashedValue = (hashedValue + name.charAt(i)) * 3074457345618258799L;
		return hashedValue;
	}

	public static long hash64(String name, int n) {
		return hash64(3074457345618258791L, name, n);
	}

	public static long hash64(String name) {
		return hash64(name, name.length());
	}

	public static long hash64(long initial, String name) {
		return hash64(initial, name, name.length());
	}

	public static long hash64(long initial, char c) {
		// This is a Knuth hash
		return (initial + c) * 3074457345618258799L;
	}

	public static int hash32(String name) {
		long hash64 = hash64(name);
		return (int)(hash64 ^ (hash64 >> 32));
	}

	// PMap<K, V>
	public static int hashLog(long initial, Class<?> key, Class<?> value) {
		return hashLog(initial, Reflect.getStableName(key), Reflect.getStableName(value));
	}

	// PMap<K, V>
	public static int hashLog(long initial, String key, String value) {
		var hash64 = hash64(initial, key);
		hash64 = hash64(hash64(hash64, ','), ' ');
		hash64 = hash64(hash64, value);
		hash64 = hash64(hash64, '>');
		return (int)(hash64 ^ (hash64 >> 32));
	}

	// PList<V>
	public static int hashLog(long initial, Class<?> value) {
		return hashLog(initial, Reflect.getStableName(value));
	}

	// PList<V>
	public static int hashLog(long initial, String value) {
		var hash64 = hash64(initial, value);
		hash64 = hash64(hash64, '>');
		return (int)(hash64 ^ (hash64 >> 32));
	}

	// 生成侧 mapKey() 在 __zeze_map_key__ 未设置前返回 null（集合放入时才写入），
	// 契约跟生成侧 @Nullable 对齐；基类自身仍恒 throw。
	public @Nullable Object mapKey() {
		throw new UnsupportedOperationException(getClass().getName());
	}

	public void mapKey(Object mapKey) {
		throw new UnsupportedOperationException(getClass().getName());
	}

	public void followerApply(Log log) {
		throw new UnsupportedOperationException(getClass().getName());
	}

	/**
	 * Create one synchronous consumer node from the explicitly supplied final savepoint state.
	 * Null means no structural log; a non-null untracked collection log must be rejected.
	 * Borrowed Beans and note collections are valid while the committing record locks are held.
	 */
	public LogBean createLogBean(@Nullable TransactionLog finalLog) {
		return new LogBean(variableId, this);
	}
	// 当Bean放到Table中时，用来支持数据版本号。
	public long version() {
		return 0;
	}

	public String versionVarName() {
		return "";
	}

	// package use in Transaction.finalCommit
	protected void version(long newVersion) {
		// 子类实现
	}

	public static String parentsToName(Collection<String> parents) {
		if (parents.isEmpty())
			return "";

		var sb = new StringBuilder();
		for (var name : parents)
			sb.append(name).append('_');
		return sb.toString();
	}

	public Bean toPrevious() {
		return this;
	}

	public ArrayList<BVariable.Data> variables() {
		return new ArrayList<>();
	}

	/** Zero means this source has no independent collection transaction log. */
	public long changeLogKey() {
		return 0;
	}
}
