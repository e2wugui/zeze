package Zeze.Transaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Builtin.HotDistribute.BVariable;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Util.KV;
import Zeze.Util.Reflect;
import Zeze.Util.Str;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Bean implements Serializable {
	public static final int OBJECT_ID_STEP = 4096; // 自增长步长。低位保留给Variable.Id。也就是，Variable.Id 最大只能是4095.
	public static final int MAX_VARIABLE_ID = OBJECT_ID_STEP - 1;
	private static final AtomicLong objectIdGen = new AtomicLong();

	@Deprecated // 这个方法应该仅用于内部。
	public static long nextObjectId() {
		return objectIdGen.addAndGet(OBJECT_ID_STEP);
	}

	private transient final long objectId = nextObjectId();

	protected transient @Nullable Record.RootInfo rootInfo;

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
		return rootInfo == null ? null : rootInfo.getTableKey();
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
	 *
	 * @param path path
	 */
	public final void buildChangeListenerPath(@NotNull ArrayList<KV<Bean, Integer>> path) {
		for (Bean parent = this.parent; parent != null; parent = parent.parent)
			path.add(KV.create(parent, variableId));
	}

	public final boolean isManaged() {
		return rootInfo != null;
	}

	public final void initRootInfoWithRedo(@NotNull Record.RootInfo rootInfo, @Nullable Bean parent) {
		if (isManaged())
			throw new HasManagedException();
		this.rootInfo = rootInfo;
		this.parent = parent;
		Transaction.whileRedo(this);
		initChildrenRootInfoWithRedo(rootInfo);
	}

	public final void initRootInfo(@NotNull Record.RootInfo rootInfo, @Nullable Bean parent) {
		if (isManaged())
			throw new HasManagedException();
		this.rootInfo = rootInfo;
		this.parent = parent;
		initChildrenRootInfo(rootInfo);
	}

	public final void resetRootInfo() {
		rootInfo = null;
		parent = null;
	}

	// 用在第一次加载Bean时，需要初始化它的root
	protected void initChildrenRootInfo(@NotNull Record.RootInfo root) {
	}

	protected void initChildrenRootInfoWithRedo(@NotNull Record.RootInfo root) {
	}

	public boolean negativeCheck() {
		return false;
	}

	// 重置Bean子类的所有字段
	public void reset() {
	}

	public @NotNull Bean copy() {
		throw new UnsupportedOperationException();
	}

	public void assign(@NotNull Data data) {
		throw new UnsupportedOperationException();
	}

	public @NotNull Data toData() {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("unchecked")
	public static <B extends Bean, D extends Data> void toBeanList(@NotNull Collection<D> fromDataList,
																   @NotNull Collection<B> toBeanList) {
		for (var data : fromDataList)
			toBeanList.add((B)data.toBean());
	}

	@SuppressWarnings("unchecked")
	public static <B extends Bean, D extends Data> void toDataList(@NotNull Collection<B> fromBeanList,
																   @NotNull Collection<D> toDataList) {
		for (var bean : fromBeanList)
			toDataList.add((D)bean.toData());
	}

	@SuppressWarnings("unchecked")
	public static <K, B extends Bean, D extends Data> void toBeanMap(@NotNull Map<K, D> fromDataMap,
																	 @NotNull Map<K, B> toBeanMap) {
		for (var e : fromDataMap.entrySet())
			toBeanMap.put(e.getKey(), (B)e.getValue().toBean());
	}

	@SuppressWarnings("unchecked")
	public static <K, B extends Bean, D extends Data> void toDataMap(@NotNull Map<K, B> fromBeanMap,
																	 @NotNull Map<K, D> toDataMap) {
		for (var e : fromBeanMap.entrySet())
			toDataMap.put(e.getKey(), (D)e.getValue().toData());
	}

	public void buildString(@NotNull StringBuilder sb, int level) {
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
	// XXX: 这个算法定好之后，就不能变了。
	public static long hash64(long initial, @NotNull String name, int n) {
		// This is a Knuth hash
		long hashedValue = initial;
		for (int i = 0; i < n; i++)
			hashedValue = (hashedValue + name.charAt(i)) * 3074457345618258799L;
		return hashedValue;
	}

	public static long hash64(@NotNull String name, int n) {
		return hash64(3074457345618258791L, name, n);
	}

	public static long hash64(@NotNull String name) {
		return hash64(name, name.length());
	}

	public static long hash64(long initial, @NotNull String name) {
		return hash64(initial, name, name.length());
	}

	public static long hash64(long initial, char c) {
		// This is a Knuth hash
		return (initial + c) * 3074457345618258799L;
	}

	public static int hash32(@NotNull String name) {
		long hash64 = hash64(name);
		return (int)(hash64 ^ (hash64 >> 32));
	}

	// PMap<K, V>
	public static int hashLog(long initial, @NotNull Class<?> key, @NotNull Class<?> value) {
		return hashLog(initial, Reflect.getStableName(key), Reflect.getStableName(value));
	}

	// PMap<K, V>
	public static int hashLog(long initial, @NotNull String key, @NotNull String value) {
		var hash64 = hash64(initial, key);
		hash64 = hash64(hash64(hash64, ','), ' ');
		hash64 = hash64(hash64, value);
		hash64 = hash64(hash64, '>');
		return (int)(hash64 ^ (hash64 >> 32));
	}

	// PList<V>
	public static int hashLog(long initial, @NotNull Class<?> value) {
		return hashLog(initial, Reflect.getStableName(value));
	}

	// PList<V>
	public static int hashLog(long initial, @NotNull String value) {
		var hash64 = hash64(initial, value);
		hash64 = hash64(hash64, '>');
		return (int)(hash64 ^ (hash64 >> 32));
	}

	public @NotNull Object mapKey() {
		throw new UnsupportedOperationException();
	}

	public void mapKey(@NotNull Object mapKey) {
		throw new UnsupportedOperationException();
	}

	public void followerApply(@NotNull Log log) {
		throw new UnsupportedOperationException();
	}

	public @NotNull LogBean createLogBean() {
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

	public static @NotNull String parentsToName(@NotNull ArrayList<String> parents) {
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

	public @NotNull ArrayList<BVariable.Data> variables() {
		return new ArrayList<>();
	}
}
