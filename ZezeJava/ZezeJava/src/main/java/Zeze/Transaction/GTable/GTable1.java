package Zeze.Transaction.GTable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.Map1Meta;
import Zeze.Transaction.Collections.Map2Meta;
import Zeze.Transaction.Collections.PMap2;
import Zeze.Transaction.Record;
import Zeze.Util.Json;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static Zeze.Util.Json.ensureNotNull;

/** 事务二维表（行键×列键→不可变值）：外层 PMap2 装 BeanMap1 行 Bean。 */
@SuppressWarnings("unchecked")
public class GTable1<R, C, V> extends StandardTable<R, C, V> {
	// coll-01：外层logTypeId/name由(row,col,val)完整身份参与（GTable1专用家族头，与PMap2的
	// LogMap2命名空间分流）——同row不同列/值类型的表不再共享typeId，History回放端Log.create
	// 按typeId查表各得其所。身份原料用getStableName（内置类型短名"int"/"string"，bean全名）。
	static final String OUTER_HEAD = "Zeze.Transaction.GTable.GTable1<";
	static final String OUTER_NAME_PREFIX = "GTable1:";

	public static final class Factory<R, C, V> implements Supplier<Map<C, V>> {
		private final @NotNull Map2Meta<R, BeanMap1<C, V>> pmapMeta;
		private final @NotNull Map1Meta<C, V> bmapMeta;
		private final @NotNull Json.FieldMeta fm1;
		private final @NotNull Json.FieldMeta fm2;

		Factory(@NotNull Map2Meta<R, BeanMap1<C, V>> pmapMeta, @NotNull Map1Meta<C, V> bmapMeta) {
			this.pmapMeta = pmapMeta;
			this.bmapMeta = bmapMeta;
			// fm1/fm2在构造期一次性构建（FND7-10）：原惰性初始化只校验fm1且两写分离，
			// 并发首次解析可观察到fm1已写、fm2未写的部分状态，parseMap0解引用null直接NPE。
			// Factory经factories的ConcurrentHashMap发布，final字段+安全发布根除该类竞态。
			// 类型实参取自metas的keyClass/valueClass：与解析期宿主字段fieldMeta.paramTypes
			// 等价，且与factory::get实际创建的容器类型一致。
			try {
				var dummyField = GTable1.class.getDeclaredField("pMap2");
				// keyParser经反射回退工厂（FND8-31）：BeanKey/binary/decimal/vector等
				// 非内建键不在keyReaderMap，裸取为null时首键解析即NPE。
				fm1 = new Json.FieldMeta(0x3c, 0, "PMap2", BeanMap1.class, this::get,
						Json.ClassMeta.getKeyReaderOrFallback(Json.instance, pmapMeta.keyClass, "GTable1 row key"),
						dummyField);
				// fm2.klass用真实valueClass（FND8-31孪生）：TYPE_CUSTOM值按Object的
				// ClassMeta解析会得到裸空Object（静默数据损坏），Binary等按真实类走
				// 自定义parser/反射字段解析，与写侧对称。
				fm2 = new Json.FieldMeta(0x30 + Json.ClassMeta.getType(bmapMeta.valueClass),
						0, "BeanMap1", bmapMeta.valueClass,
						Json.ClassMeta.getDefCtor(bmapMeta.valueClass),
						Json.ClassMeta.getKeyReaderOrFallback(Json.instance, bmapMeta.keyClass, "GTable1 column key"),
						dummyField);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException(e);
			}
		}

		public @NotNull Map2Meta<R, BeanMap1<C, V>> getPmapMeta() {
			return pmapMeta;
		}

		public @NotNull Map1Meta<C, V> getBmapMeta() {
			return bmapMeta;
		}

		@Override
		public @NotNull Map<C, V> get() {
			return new BeanMap1<>(bmapMeta);
		}
	}

	static {
		var json = Json.instance;

		json.getClassMeta(GTable1.class).setParser((reader, classMeta, fieldMeta, obj, parent) -> {
			if (obj == null || fieldMeta == null)
				return null; // 不支持GTable1新构造和非字段的GTable1对象
			obj.pMap2.clear();
			var factory = (Factory<?, ?, ?>)obj.factory;
			var fm1 = factory.fm1;
			var fm2 = factory.fm2;
			var keyParser = ensureNotNull(fm1.keyParser);
			for (int b = reader.skipNext(); b != '}'; b = reader.skipVar('}')) {
				var k = keyParser.parse(reader, b);
				reader.skipColon();
				var map = (BeanMap1<Object, Object>)ensureNotNull(fm1.ctor).create();
				reader.parseMap0(map, classMeta, fm2);
				obj.pMap2.put(k, map);
			}
			reader.pos(reader.pos() + 1);
			return obj;
		});
		json.getClassMeta(GTable1.class).setWriter((writer, classMeta, obj) ->
				writer.write(json, obj != null ? obj.pMap2 : null));
	}

	private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, Factory<?, ?, ?>>>>
			factories = new ConcurrentHashMap<>();

	private final @NotNull PMap2<R, BeanMap1<C, V>> pMap2;

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

	public void assign(GTable1<R, C, V> other) {
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
		_s_.append("Zeze.Transaction.GTable.GTable1: {\n");
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

	// Bean行/列键显式拒绝（R3-T复审C2显式化）：工厂层已拦（Map1Meta/Map2Meta.checkNonBeanKey），但报错深在
	// getFactory内部且为"LogMap2/LogMap1 ..."家族名——GTable1的行键走Map2Meta.create会误报
	// "LogMap2"，不点名GTable也不指明行/列维度。Bean是值语义equals配身份hashCode，行/列任一
	// Bean维度的哈希put/get/contains失真；schema合法键只有内建类型与BeanKey（Gen/Types/
	// TypeGTable.cs要求IsKeyable，Types.Bean的IsKeyable=false）。前置报错点名维度。
	private static void checkNonBeanDimension(@NotNull String dimension, @NotNull Class<?> keyClass) {
		if (Bean.class.isAssignableFrom(keyClass))
			throw new IllegalArgumentException(
					"GTable1 does not support Bean " + dimension + " key type (equals-without-hashCode misbehaves in hash map), use BeanKey: "
							+ keyClass.getName());
	}

	public GTable1(@NotNull Class<R> rowClass, @NotNull Class<C> colClass, @NotNull Class<V> valClass) {
		checkNonBeanDimension("row", rowClass);
		checkNonBeanDimension("column", colClass);
		// Bean值不支持（FND7-83，PList1/PMap1拒绝Bean值判例同族）：GTable1为动态标量值
		// 设计（bean值由GTable2的带valueClass路径承担），Bean值需要GTable2的BeanMap2
		// 元数据机制才能正确解码。schema层bean值恒产GTable2（History.Helper.
		// dependsGTable），仅手写可触发，显式失败优于静默数据错误。
		if (Bean.class.isAssignableFrom(valClass))
			throw new IllegalArgumentException(
					"GTable1 does not support Bean value type (json parse yields bare empty Object), use GTable2: "
							+ valClass.getName());
		var factory = getFactory(rowClass, colClass, valClass);
		this.pMap2 = new PMap2<>(factory.pmapMeta);
		super.backingMap = (Map<R, Map<C, V>>)(Map<?, ?>)pMap2;
		super.factory = factory;
	}

	// Factory唯一来源是getFactory（meta经工厂层Bean拦截），不再重复维度/值check。
	// 供生成代码常量化（static final Factory + 本构造器），消除每实例化的三层缓存探测。
	public GTable1(@NotNull Factory<R, C, V> factory) {
		this.pMap2 = new PMap2<>(factory.pmapMeta);
		super.backingMap = (Map<R, Map<C, V>>)(Map<?, ?>)pMap2;
		super.factory = factory;
	}

	public static <R, C, V> @NotNull Factory<R, C, V> getFactory(
			@NotNull Class<R> rowClass, @NotNull Class<C> colClass, @NotNull Class<V> valClass) {
		var map = factories.computeIfAbsent(rowClass, __ -> new ConcurrentHashMap<>())
				.computeIfAbsent(colClass, __ -> new ConcurrentHashMap<>());
		var factory = map.get(valClass);
		if (factory == null) {
			var bmapMeta = Map1Meta.get(colClass, valClass);
			var pmapMeta = Map2Meta.createWithFamily(OUTER_HEAD, OUTER_NAME_PREFIX, rowClass,
					(Class<BeanMap1<C, V>>)(Class<?>)BeanMap1.class,
					Zeze.Util.Reflect.getStableName(colClass) + ", " + Zeze.Util.Reflect.getStableName(valClass),
					() -> new BeanMap1<>(bmapMeta));
			factory = map.computeIfAbsent(valClass, __ -> new Factory<>(pmapMeta, bmapMeta));
		}
		return (Factory<R, C, V>)factory;
	}

	public @NotNull PMap2<R, BeanMap1<C, V>> getPMap2() {
		return pMap2;
	}
}
