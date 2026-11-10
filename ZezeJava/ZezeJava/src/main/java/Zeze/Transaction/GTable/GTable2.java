package Zeze.Transaction.GTable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import Zeze.Transaction.Bean;
import Zeze.Transaction.DynamicField;
import Zeze.Transaction.Collections.Map2Meta;
import Zeze.Transaction.Collections.PMap2;
import Zeze.Transaction.Record;
import Zeze.Util.Json;
import org.jspecify.annotations.Nullable;

import static Zeze.Util.Json.ensureNotNull;

/** 保存受管 Bean 值的事务二维表，外层 PMap2 保存 BeanMap2 行。行键和列键按引用保存，须不可变。 */
@SuppressWarnings("unchecked")
public class GTable2<R, C, V extends Bean, VReadOnly> extends StandardTable<R, C, V> {
	// 外层日志类型号包含表家族及行、列、值类型。共享类型号的动态表按字段选择解码器。
	static final String OUTER_HEAD = "Zeze.Transaction.GTable.GTable2<";
	static final String OUTER_NAME_PREFIX = "GTable2:";

	public static final class Factory<R, C, V extends Bean, VReadOnly> implements Supplier<Map<C, V>> {
		private final Map2Meta<R, BeanMap2<C, V, VReadOnly>> pmapMeta;
		private final Map2Meta<C, V> bmapMeta;
		private final Json.FieldMeta fm1;
		private final Json.FieldMeta fm2;

		private Factory(Map2Meta<R, BeanMap2<C, V, VReadOnly>> pmapMeta, Map2Meta<C, V> bmapMeta) {
			this.pmapMeta = pmapMeta;
			this.bmapMeta = bmapMeta;
			// 工厂发布前一次性初始化两层 JSON 元数据。
			try {
				var dummyField = GTable2.class.getDeclaredField("pMap2");
				fm1 = new Json.FieldMeta(0x3c, 0, "PMap2", BeanMap2.class, this::get,
					Json.ClassMeta.getKeyReaderOrFallback(Json.instance, pmapMeta.keyClass, "GTable2 row key"),
					dummyField);
				// 动态值由内层 Map 的字段工厂创建。
				fm2 = new Json.FieldMeta(0x3c, 0, "BeanMap2", bmapMeta.valueClass,
					bmapMeta.valueClass == Zeze.Transaction.DynamicBean.class
						? null : Json.ClassMeta.getDefCtor(bmapMeta.valueClass),
					Json.ClassMeta.getKeyReaderOrFallback(Json.instance, bmapMeta.keyClass, "GTable2 column key"),
					dummyField);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException(e);
			}
		}

		public Map2Meta<R, BeanMap2<C, V, VReadOnly>> getPmapMeta() {
			return pmapMeta;
		}

		public Map2Meta<C, V> getBmapMeta() {
			return bmapMeta;
		}

		@Override
		public Map<C, V> get() {
			return new BeanMap2<>(bmapMeta);
		}
	}

	static {
		var json = Json.instance;

		json.getClassMeta(GTable2.class).setParser((reader, classMeta, fieldMeta, obj, parent) -> {
			if (obj == null || fieldMeta == null)
				return null; // 只解析已初始化的表字段。
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
				if (!map.isEmpty())
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

	private final PMap2<R, BeanMap2<C, V, VReadOnly>> pMap2;

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

	public void assign(GTable2<R, C, V, VReadOnly> other) {
		if (this == other)
			return;
		// 用目标工厂复制所有行，成功后再替换原内容。
		var rows = new HashMap<R, BeanMap2<C, V, VReadOnly>>();
		for (var entry : other.pMap2.entrySet()) {
			var row = (BeanMap2<C, V, VReadOnly>)factory.get();
			row.assign(entry.getValue());
			rows.put(entry.getKey(), row);
		}
		pMap2.clear();
		pMap2.putAll(rows);
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

	private static void checkNonBeanDimension(String dimension, Class<?> keyClass) {
		if (Bean.class.isAssignableFrom(keyClass))
			throw new IllegalArgumentException(
				"GTable2 does not support Bean " + dimension + " key type (equals-without-hashCode misbehaves in hash map), use BeanKey: "
					+ keyClass.getName());
	}

	public GTable2(Class<R> rowClass, Class<C> colClass, Class<V> valClass) {
		checkNonBeanDimension("row", rowClass);
		checkNonBeanDimension("column", colClass);
		var factory = GTable2.<R, C, V, VReadOnly>getFactory(rowClass, colClass, valClass);
		this.pMap2 = new PMap2<>(factory.pmapMeta);
		super.backingMap = (Map<R, Map<C, V>>)(Map<?, ?>)pMap2;
		super.factory = factory;
	}

	/** 复用工厂，省去实例化时的缓存查找。 */
	public GTable2(Factory<R, C, V, VReadOnly> factory) {
		this.pMap2 = new PMap2<>(factory.pmapMeta);
		super.backingMap = (Map<R, Map<C, V>>)(Map<?, ?>)pMap2;
		super.factory = factory;
	}

	public static <R, C, V extends Bean, VReadOnly> Factory<R, C, V, VReadOnly> getFactory(
		Class<R> rowClass, Class<C> colClass, Class<V> valClass) {
		if (valClass == Zeze.Transaction.DynamicBean.class)
			throw new IllegalArgumentException(
				"GTable2 does not support DynamicBean value via this overload (no default constructor),"
					+ " use getFactory(rowClass, colClass, dynamicField): " + valClass.getName());
		var map = factories.computeIfAbsent(rowClass, __ -> new ConcurrentHashMap<>())
			.computeIfAbsent(colClass, __ -> new ConcurrentHashMap<>());
		var factory = map.get(valClass);
		if (factory == null) {
			factory = map.computeIfAbsent(valClass, __ -> createFactory(rowClass, Map2Meta.get(colClass, valClass), null));
		}
		return (Factory<R, C, V, VReadOnly>)factory;
	}

	public static <R, C, VReadOnly> Factory<R, C, Zeze.Transaction.DynamicBean, VReadOnly> getFactory(
		Class<R> rowClass, Class<C> colClass, DynamicField field) {
		return createFactory(rowClass, Map2Meta.createDynamic(colClass, field), field);
	}

	private static <R, C, V extends Bean, VReadOnly> Factory<R, C, V, VReadOnly> createFactory(
		Class<R> rowClass, Map2Meta<C, V> bmapMeta, @Nullable DynamicField field) {
		var pmapMeta = Map2Meta.createWithFamily(OUTER_HEAD, OUTER_NAME_PREFIX, rowClass,
			(Class<BeanMap2<C, V, VReadOnly>>)(Class<?>)BeanMap2.class,
			Zeze.Util.Reflect.getStableName(bmapMeta.keyClass) + ", "
				+ Zeze.Util.Reflect.getStableName(bmapMeta.valueClass),
			() -> new BeanMap2<>(bmapMeta), field, row -> {
				if (row.getPMap2().getMeta() != bmapMeta)
					throw new IllegalArgumentException("GTable2 row Meta does not match the target table");
			});
		return new Factory<>(pmapMeta, bmapMeta);
	}

	/**
	 * 供生成代码和框架使用的底层行 Map。写入的行须由目标工厂创建，并包含至少一个单元格。
	 * 常规读写走 {@link Table}。
	 */
	public PMap2<R, BeanMap2<C, V, VReadOnly>> getPMap2() {
		return pMap2;
	}
}
