package Zeze.Transaction.GTable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.Map2Meta;
import Zeze.Transaction.Collections.PMap2;
import Zeze.Transaction.Record;
import Zeze.Util.Json;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static Zeze.Util.Json.ensureNotNull;

/** 事务二维表（行键×列键→受管 Bean 值）：外层 PMap2 装 BeanMap2 行 Bean。 */
@SuppressWarnings("unchecked")
public class GTable2<R, C, V extends Bean, VReadOnly> extends StandardTable<R, C, V> {
	public static final class Factory<R, C, V extends Bean, VReadOnly> implements Supplier<Map<C, V>> {
		private final @NotNull Map2Meta<R, BeanMap2<C, V, VReadOnly>> pmapMeta;
		private final @NotNull Map2Meta<C, V> bmapMeta;
		private final @NotNull Json.FieldMeta fm1;
		private final @NotNull Json.FieldMeta fm2;

		Factory(@NotNull Map2Meta<R, BeanMap2<C, V, VReadOnly>> pmapMeta, @NotNull Map2Meta<C, V> bmapMeta) {
			this.pmapMeta = pmapMeta;
			this.bmapMeta = bmapMeta;
			// fm1/fm2在构造期一次性构建（FND7-10）：原惰性初始化只校验fm1且两写分离，
			// 并发首次解析可观察到fm1已写、fm2未写的部分状态，parseMap0解引用null直接NPE。
			// Factory经factories的ConcurrentHashMap发布，final字段+安全发布根除该类竞态。
			// 类型实参取自metas的keyClass/valueClass：与解析期宿主字段fieldMeta.paramTypes
			// 等价，且与factory::get实际创建的容器类型一致。V必为Bean，fm2类型恒MAP+CUSTOM。
			try {
				var dummyField = GTable2.class.getDeclaredField("pMap2");
				// keyParser经反射回退工厂（FND8-31）：BeanKey等非内建键不在
				// keyReaderMap，裸取为null时首键解析即NPE。
				fm1 = new Json.FieldMeta(0x3c, 0, "PMap2", BeanMap2.class, this::get,
						Json.ClassMeta.getKeyReaderOrFallback(Json.instance, pmapMeta.keyClass, "GTable2 row key"),
						dummyField);
				// DynamicBean不走getDefCtor（无无参构造器，allocateInstance兜底产出
				// getBean/createBean均null的未初始化实例）；该ctor在本解析路径不使用，传null。
				fm2 = new Json.FieldMeta(0x3c, 0, "BeanMap2", bmapMeta.valueClass,
						bmapMeta.valueClass == Zeze.Transaction.DynamicBean.class
								? null : Json.ClassMeta.getDefCtor(bmapMeta.valueClass),
						Json.ClassMeta.getKeyReaderOrFallback(Json.instance, bmapMeta.keyClass, "GTable2 column key"),
						dummyField);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException(e);
			}
		}

		public @NotNull Map2Meta<R, BeanMap2<C, V, VReadOnly>> getPmapMeta() {
			return pmapMeta;
		}

		public @NotNull Map2Meta<C, V> getBmapMeta() {
			return bmapMeta;
		}

		@Override
		public @NotNull Map<C, V> get() {
			return new BeanMap2<>(bmapMeta);
		}
	}

	static {
		var json = Json.instance;

		json.getClassMeta(GTable2.class).setParser((reader, classMeta, fieldMeta, obj, parent) -> {
			if (obj == null || fieldMeta == null)
				return null; // 不支持GTable2新构造和非字段的GTable2对象
			obj.pMap2.clear();
			var factory = (Factory<?, ?, ?, ?>)obj.factory;
			var fm1 = factory.fm1;
			var fm2 = factory.fm2;
			var keyParser = ensureNotNull(fm1.keyParser);
			for (int b = reader.skipNext(); b != '}'; b = reader.skipVar('}')) {
				var k = keyParser.parse(reader, b);
				reader.skipColon();
				//noinspection rawtypes
				var map = (BeanMap2)ensureNotNull(fm1.ctor).create();
				reader.parseMap0(map, classMeta, fm2);
				obj.pMap2.put(k, map);
			}
			reader.pos(reader.pos() + 1);
			return obj;
		});
		json.getClassMeta(GTable2.class).setWriter((writer, classMeta, obj) ->
				writer.write(json, obj != null ? obj.pMap2 : null));
	}

	private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, Factory<?, ?, ?, ?>>>>
			factories = new ConcurrentHashMap<>();

	private final @NotNull PMap2<R, BeanMap2<C, V, VReadOnly>> pMap2;

	@SuppressWarnings("deprecation")
	public void variableId(int id) {
		pMap2.variableId(id);
	}

	public final void initRootInfoWithRedo(@NotNull Record.RootInfo rootInfo, @Nullable Bean parent) {
		pMap2.initRootInfoWithRedo(rootInfo, parent);
	}

	public final void initRootInfo(@NotNull Record.RootInfo rootInfo, @Nullable Bean parent) {
		pMap2.initRootInfo(rootInfo, parent);
	}

	/*
	protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
		pMap2.initRootInfo(_r_, this);
	}

	protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
		pMap2.initRootInfoWithRedo(_r_, this);
	}
	*/

	public void assign(GTable2<R, C, V, VReadOnly> other) {
		pMap2.clear();
		for (var _e_ : other.pMap2.entrySet())
			pMap2.put(_e_.getKey(), _e_.getValue().copy());
	}

	@Override
	public String toString() {
		var _s_ = new StringBuilder();
		buildString(_s_, 0);
		return _s_.toString();
	}

	public void buildString(StringBuilder _s_, int _l_) {
		var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
		var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
		_s_.append("Zeze.Transaction.GTable.GTable2: {\n");
		_s_.append(_i1_).append("PMap2={");
		if (!pMap2.isEmpty()) {
			_s_.append('\n');
			for (var _e_ : pMap2.entrySet()) {
				_s_.append(_i2_).append("RowKey=").append(_e_.getKey()).append(",\n");
				_s_.append(_i2_).append("Value=").append(_e_.getValue()).append(",\n");
			}
			_s_.append(_i1_);
		}
		_s_.append("}\n");
		_s_.append(Zeze.Util.Str.indent(_l_)).append('}');
	}

	// Bean行/列键显式拒绝（R3-T复审C2显式化）：工厂层已拦（Map2Meta.checkNonBeanKey），但报错深在
	// getFactory内部且文案是"LogMap2 ..."家族名——不点名GTable也不指明行/列维度。Bean是值语义
	// equals配身份hashCode（可变bean不覆写hashCode防哈希漂移），行/列任一Bean维度的哈希
	// put/get/contains失真；schema合法键只有内建类型与BeanKey（Gen/Types/TypeGTable.cs要求
	// IsKeyable，Types.Bean的IsKeyable=false）。前置报错点名维度，工厂层拦截保留（防御纵深）。
	private static void checkNonBeanDimension(@NotNull String dimension, @NotNull Class<?> keyClass) {
		if (Bean.class.isAssignableFrom(keyClass))
			throw new IllegalArgumentException(
					"GTable2 does not support Bean " + dimension + " key type (equals-without-hashCode misbehaves in hash map), use BeanKey: "
							+ keyClass.getName());
	}

	public GTable2(@NotNull Class<R> rowClass, @NotNull Class<C> colClass, @NotNull Class<V> valClass) {
		checkNonBeanDimension("row", rowClass);
		checkNonBeanDimension("column", colClass);
		var factory = GTable2.<R, C, V, VReadOnly>getFactory(rowClass, colClass, valClass);
		this.pMap2 = new PMap2<>(factory.pmapMeta);
		super.backingMap = (Map<R, Map<C, V>>)(Map<?, ?>)pMap2;
		super.factory = factory;
	}

	// Factory唯一来源是getFactory（meta经工厂层Bean拦截），不再重复维度check。
	// 供生成代码常量化（static final Factory + 本构造器），消除每实例化的三层缓存探测。
	public GTable2(@NotNull Factory<R, C, V, VReadOnly> factory) {
		this.pMap2 = new PMap2<>(factory.pmapMeta);
		super.backingMap = (Map<R, Map<C, V>>)(Map<?, ?>)pMap2;
		super.factory = factory;
	}

	public static <R, C, V extends Bean, VReadOnly> @NotNull Factory<R, C, V, VReadOnly> getFactory(
			@NotNull Class<R> rowClass, @NotNull Class<C> colClass, @NotNull Class<V> valClass) {
		// 【FND8-33】DynamicBean没有无参构造器，Map2Meta.get的深反射抛不带
		// "dynamic不支持"信息的NoSuchMethodException——指名拒绝，dynamic值走带
		// get/create工厂的重载。
		if (valClass == Zeze.Transaction.DynamicBean.class)
			throw new IllegalArgumentException(
					"GTable2 does not support DynamicBean value via this overload (no default constructor),"
							+ " use getFactory(rowClass, colClass, get, create): " + valClass.getName());
		var map = factories.computeIfAbsent(rowClass, __ -> new ConcurrentHashMap<>())
				.computeIfAbsent(colClass, __ -> new ConcurrentHashMap<>());
		var factory = map.get(valClass);
		if (factory == null) {
			var bmapMeta = Map2Meta.get(colClass, valClass);
			var pmapMeta = Map2Meta.create(rowClass, (Class<BeanMap2<C, V, VReadOnly>>)(Class<?>)BeanMap2.class,
					() -> new BeanMap2<>(bmapMeta));
			factory = map.computeIfAbsent(valClass, __ -> new Factory<>(pmapMeta, bmapMeta));
		}
		return (Factory<R, C, V, VReadOnly>)factory;
	}

	// 【FND8-33 A1】dynamic值的工厂路径（对齐PMap2的dynamic构造器判例）：工厂按变量
	// 成对（不同变量不同工厂），不进按类缓存。bmapMeta与Helper.registerLogMap2Dynamic
	// 注册的meta同函数同typeId（Log.register先到先得的等价契约）；pmapMeta与三参版
	// 路径完全一致。
	public static <R, C, VReadOnly> @NotNull Factory<R, C, Zeze.Transaction.DynamicBean, VReadOnly> getFactory(
			@NotNull Class<R> rowClass, @NotNull Class<C> colClass,
			@NotNull java.util.function.ToLongFunction<Bean> get,
			@NotNull java.util.function.LongFunction<Bean> create) {
		var bmapMeta = Map2Meta.<C, Zeze.Transaction.DynamicBean>createDynamic(colClass, get, create);
		var pmapMeta = Map2Meta.create(rowClass,
				(Class<BeanMap2<C, Zeze.Transaction.DynamicBean, VReadOnly>>)(Class<?>)BeanMap2.class,
				() -> new BeanMap2<>(bmapMeta));
		return new Factory<>(pmapMeta, bmapMeta);
	}

	public @NotNull PMap2<R, BeanMap2<C, V, VReadOnly>> getPMap2() {
		return pMap2;
	}
}
