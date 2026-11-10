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
import org.jspecify.annotations.Nullable;

import static Zeze.Util.Json.ensureNotNull;

/** 按值记录变更的事务二维表，外层 PMap2 保存 BeanMap1 行。行键和列键按引用保存，须不可变。 */
@SuppressWarnings("unchecked")
public class GTable1<R, C, V> extends StandardTable<R, C, V> {
	// 外层日志类型号包含表家族及行键、列键和值的类型。
	static final String OUTER_HEAD = "Zeze.Transaction.GTable.GTable1<";
	static final String OUTER_NAME_PREFIX = "GTable1:";

	public static final class Factory<R, C, V> implements Supplier<Map<C, V>> {
		private final Map2Meta<R, BeanMap1<C, V>> pmapMeta;
		private final Map1Meta<C, V> bmapMeta;
		private final Json.FieldMeta fm1;
		private final Json.FieldMeta fm2;

		private Factory(Map2Meta<R, BeanMap1<C, V>> pmapMeta, Map1Meta<C, V> bmapMeta) {
			this.pmapMeta = pmapMeta;
			this.bmapMeta = bmapMeta;
			// 工厂发布前一次性初始化两层 JSON 元数据。
			try {
				var dummyField = GTable1.class.getDeclaredField("pMap2");
				fm1 = new Json.FieldMeta(0x3c, 0, "PMap2", BeanMap1.class, this::get,
					Json.ClassMeta.getKeyReaderOrFallback(Json.instance, pmapMeta.keyClass, "GTable1 row key"),
					dummyField);
				// 自定义值按实际类型选择 JSON 解析器。
				fm2 = new Json.FieldMeta(0x30 + Json.ClassMeta.getType(bmapMeta.valueClass),
					0, "BeanMap1", bmapMeta.valueClass,
					Json.ClassMeta.getDefCtor(bmapMeta.valueClass),
					Json.ClassMeta.getKeyReaderOrFallback(Json.instance, bmapMeta.keyClass, "GTable1 column key"),
					dummyField);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException(e);
			}
		}

		public Map2Meta<R, BeanMap1<C, V>> getPmapMeta() {
			return pmapMeta;
		}

		public Map1Meta<C, V> getBmapMeta() {
			return bmapMeta;
		}

		@Override
		public Map<C, V> get() {
			return new BeanMap1<>(bmapMeta);
		}
	}

	static {
		var json = Json.instance;

		json.getClassMeta(GTable1.class).setParser((reader, classMeta, fieldMeta, obj, parent) -> {
			if (obj == null || fieldMeta == null)
				return null; // 只解析已初始化的表字段。
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
				if (!map.isEmpty())
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

	private final PMap2<R, BeanMap1<C, V>> pMap2;

	@SuppressWarnings("deprecation")
	public void variableId(int id) {
		pMap2.variableId(id);
	}

	public final void initRootInfoWithRedo(Record.RootInfo rootInfo, @Nullable Bean parent) {
		pMap2.initRootInfoWithRedo(rootInfo, parent);
	}

	public final void initRootInfo(Record.RootInfo rootInfo, @Nullable Bean parent) {
		pMap2.initRootInfo(rootInfo, parent);
	}

	public void assign(GTable1<R, C, V> other) {
		if (this == other)
			return;
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

	private static void checkNonBeanDimension(String dimension, Class<?> keyClass) {
		if (Bean.class.isAssignableFrom(keyClass))
			throw new IllegalArgumentException(
				"GTable1 does not support Bean " + dimension + " key type (equals-without-hashCode misbehaves in hash map), use BeanKey: "
					+ keyClass.getName());
	}

	public GTable1(Class<R> rowClass, Class<C> colClass, Class<V> valClass) {
		checkNonBeanDimension("row", rowClass);
		checkNonBeanDimension("column", colClass);
		if (Bean.class.isAssignableFrom(valClass))
			throw new IllegalArgumentException(
				"GTable1 does not support Bean value type (json parse yields bare empty Object), use GTable2: "
					+ valClass.getName());
		var factory = getFactory(rowClass, colClass, valClass);
		this.pMap2 = new PMap2<>(factory.pmapMeta);
		super.backingMap = (Map<R, Map<C, V>>)(Map<?, ?>)pMap2;
		super.factory = factory;
	}

	/** 复用工厂，省去实例化时的缓存查找。 */
	public GTable1(Factory<R, C, V> factory) {
		this.pMap2 = new PMap2<>(factory.pmapMeta);
		super.backingMap = (Map<R, Map<C, V>>)(Map<?, ?>)pMap2;
		super.factory = factory;
	}

	public static <R, C, V> Factory<R, C, V> getFactory(
		Class<R> rowClass, Class<C> colClass, Class<V> valClass) {
		var map = factories.computeIfAbsent(rowClass, __ -> new ConcurrentHashMap<>())
			.computeIfAbsent(colClass, __ -> new ConcurrentHashMap<>());
		var factory = map.get(valClass);
		if (factory == null) {
			var bmapMeta = Map1Meta.get(colClass, valClass);
			var pmapMeta = Map2Meta.createWithFamily(OUTER_HEAD, OUTER_NAME_PREFIX, rowClass,
				(Class<BeanMap1<C, V>>)(Class<?>)BeanMap1.class,
				Zeze.Util.Reflect.getStableName(colClass) + ", " + Zeze.Util.Reflect.getStableName(valClass),
				() -> new BeanMap1<>(bmapMeta), null, row -> {
					if (row.getPMap1().getMeta() != bmapMeta)
						throw new IllegalArgumentException("GTable1 row Meta does not match the target table");
				});
			factory = map.computeIfAbsent(valClass, __ -> new Factory<>(pmapMeta, bmapMeta));
		}
		return (Factory<R, C, V>)factory;
	}

	/**
	 * 供生成代码和框架使用的底层行 Map。写入的行须由目标工厂创建，并包含至少一个单元格。
	 * 常规读写走 {@link Table}。
	 */
	public PMap2<R, BeanMap1<C, V>> getPMap2() {
		return pMap2;
	}
}
