package Zeze.Transaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

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
	 * 构建 ChangeListener 路径（每项为父 Bean 与当前层子 Bean 的 variableId）；首项由调用方预先加入。
	 */
	public final void buildChangeListenerPath(ArrayList<KV<Bean, Integer>> path) {
		for (Bean child = this, parent = this.parent; parent != null; child = parent, parent = parent.parent)
			path.add(KV.create(parent, child.variableId));
	}

	public final boolean isManaged() {
		return rootInfo != null;
	}

	// 加入受管容器即登记 rootInfo；仅 redo 路径重置所有权，终局回滚不解除（复用须重建或 copy）。
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

	// Bean 类型 Id（用于 dynamic 等场景）；生成类总是覆写，默认按类名哈希。
	@Override
	public long typeId() {
		return hash64(getClass().getName());
	}

	// 持久化 TypeId 依赖此 Knuth 哈希算法，不得变更。
	public static long hash64(long initial, String name, int n) {
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

	// 生成子类在放入集合前返回 null，基类不支持。
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
	 * 基于最终保存点日志创建变更消费节点；null 表示无结构日志，借用的 Bean 与集合笔记仅在提交持锁期间有效。
	 */
	public LogBean createLogBean(@Nullable TransactionLog finalLog) {
		return new LogBean(variableId, this);
	}

	// 表记录数据版本号，供子类覆写。
	public long version() {
		return 0;
	}

	public String versionVarName() {
		return "";
	}

	// 供同包 Transaction.finalCommit 更新版本号
	protected void version(long newVersion) {
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

	public ArrayList<VariableMeta> variables() {
		return new ArrayList<>();
	}

	/** 返回 0 表示无独立的集合事务日志。 */
	public long changeLogKey() {
		return 0;
	}
}
