package Zeze.Transaction.GTable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Transaction.Collections.Meta2;
import Zeze.Transaction.Collections.PMap2;
import Zeze.Util.Json;
import com.google.common.base.Supplier;
import org.jetbrains.annotations.NotNull;
import static Zeze.Util.Json.ensureNotNull;

@SuppressWarnings("unchecked")
public class GTable1<R, C, V> extends StandardTable<R, C, V> {
	private static final class Factory<R, C, V> implements Supplier<Map<C, V>> {
		private final @NotNull Meta2<R, BeanMap1<C, V>> pmapMeta;
		private final @NotNull Meta2<C, V> bmapMeta;
		private Json.FieldMeta fm1, fm2;

		Factory(@NotNull Meta2<R, BeanMap1<C, V>> pmapMeta, @NotNull Meta2<C, V> bmapMeta) {
			this.pmapMeta = pmapMeta;
			this.bmapMeta = bmapMeta;
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
			if (fm1 == null) {
				var dummyField = GTable1.class.getDeclaredField("pMap2");
				factory.fm1 = fm1 = new Json.FieldMeta(0x3c, 0, "PMap2", BeanMap1.class, factory::get,
						Json.ClassMeta.getKeyReader((Class<?>)fieldMeta.paramTypes[0]), dummyField);
				factory.fm2 = fm2 = new Json.FieldMeta(0x30 + Json.ClassMeta.getType((Class<?>)fieldMeta.paramTypes[2]),
						0, "BeanMap1", Object.class,
						Json.ClassMeta.getDefCtor((Class<?>)ensureNotNull(fieldMeta.paramTypes[2])),
						Json.ClassMeta.getKeyReader((Class<?>)fieldMeta.paramTypes[1]), dummyField);
			}
			var keyParser = ensureNotNull(fm1.keyParser);
			for (int b = reader.skipNext(); b != '}'; b = reader.skipVar('}')) {
				var k = keyParser.parse(reader, b);
				reader.skipColon();
				var map = (BeanMap1<Object, Object>)ensureNotNull(fm1.ctor).create();
				reader.parseMap0(json, map, classMeta, fm2);
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

	public void variableId(int id) {
		pMap2.variableId(id);
	}

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

	public GTable1(@NotNull Class<R> rowClass, @NotNull Class<C> colClass, @NotNull Class<V> valClass) {
		var factory = getFactory(rowClass, colClass, valClass);
		this.pMap2 = new PMap2<>(factory.pmapMeta);
		super.backingMap = (Map<R, Map<C, V>>)(Map<?, ?>)pMap2;
		super.factory = factory;
	}

	private static <R, C, V> @NotNull Factory<R, C, V> getFactory(
			@NotNull Class<R> rowClass, @NotNull Class<C> colClass, @NotNull Class<V> valClass) {
		var map = factories.computeIfAbsent(rowClass, __ -> new ConcurrentHashMap<>())
				.computeIfAbsent(colClass, __ -> new ConcurrentHashMap<>());
		var factory = map.get(valClass);
		if (factory == null) {
			var bmapMeta = Meta2.getMap1Meta(colClass, valClass);
			var pmapMeta = Meta2.createMap2Meta(rowClass, (Class<BeanMap1<C, V>>)(Class<?>)BeanMap1.class,
					() -> new BeanMap1<>(bmapMeta));
			factory = map.computeIfAbsent(valClass, __ -> new Factory<>(pmapMeta, bmapMeta));
		}
		return (Factory<R, C, V>)factory;
	}

	public @NotNull PMap2<R, BeanMap1<C, V>> getPMap2() {
		return pMap2;
	}
}
