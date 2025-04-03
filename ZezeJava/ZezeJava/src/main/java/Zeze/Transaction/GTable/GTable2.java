package Zeze.Transaction.GTable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.Meta2;
import Zeze.Transaction.Collections.PMap2;
import Zeze.Transaction.Record;
import Zeze.Util.Json;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static Zeze.Util.Json.ensureNotNull;

@SuppressWarnings("unchecked")
public class GTable2<R, C, V extends Bean, VReadOnly> extends StandardTable<R, C, V> {
	public static final class Factory<R, C, V extends Bean, VReadOnly> implements Supplier<Map<C, V>> {
		private final @NotNull Meta2<R, BeanMap2<C, V, VReadOnly>> pmapMeta;
		private final @NotNull Meta2<C, V> bmapMeta;
		private Json.FieldMeta fm1, fm2;

		Factory(@NotNull Meta2<R, BeanMap2<C, V, VReadOnly>> pmapMeta, @NotNull Meta2<C, V> bmapMeta) {
			this.pmapMeta = pmapMeta;
			this.bmapMeta = bmapMeta;
		}

		public @NotNull Meta2<R, BeanMap2<C, V, VReadOnly>> getPmapMeta() {
			return pmapMeta;
		}

		public @NotNull Meta2<C, V> getBmapMeta() {
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
			if (fm1 == null) {
				var dummyField = GTable2.class.getDeclaredField("pMap2");
				var valueClass = (Class<?>)ensureNotNull(fieldMeta.paramTypes[2]);
				factory.fm1 = fm1 = new Json.FieldMeta(0x3c, 0, "PMap2", BeanMap2.class, factory::get,
						Json.ClassMeta.getKeyReader((Class<?>)fieldMeta.paramTypes[0]), dummyField);
				factory.fm2 = fm2 = new Json.FieldMeta(0x3c, 0, "BeanMap2", valueClass,
						Json.ClassMeta.getDefCtor(valueClass),
						Json.ClassMeta.getKeyReader((Class<?>)fieldMeta.paramTypes[1]), dummyField);
			}
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

	public GTable2(@NotNull Class<R> rowClass, @NotNull Class<C> colClass, @NotNull Class<V> valClass) {
		var factory = getFactory(rowClass, colClass, valClass);
		this.pMap2 = new PMap2<>((Meta2<R, BeanMap2<C, V, VReadOnly>>)(Meta2<?, ?>)factory.pmapMeta);
		super.backingMap = (Map<R, Map<C, V>>)(Map<?, ?>)pMap2;
		super.factory = factory;
	}

	public static <R, C, V extends Bean, VReadOnly> @NotNull Factory<R, C, V, VReadOnly> getFactory(
			@NotNull Class<R> rowClass, @NotNull Class<C> colClass, @NotNull Class<V> valClass) {
		var map = factories.computeIfAbsent(rowClass, __ -> new ConcurrentHashMap<>())
				.computeIfAbsent(colClass, __ -> new ConcurrentHashMap<>());
		var factory = map.get(valClass);
		if (factory == null) {
			var bmapMeta = Meta2.getMap2Meta(colClass, valClass);
			var pmapMeta = Meta2.createMap2Meta(rowClass, (Class<BeanMap2<C, V, VReadOnly>>)(Class<?>)BeanMap2.class,
					() -> new BeanMap2<>(bmapMeta));
			factory = map.computeIfAbsent(valClass, __ -> new Factory<>(pmapMeta, bmapMeta));
		}
		return (Factory<R, C, V, VReadOnly>)factory;
	}

	public @NotNull PMap2<R, BeanMap2<C, V, VReadOnly>> getPMap2() {
		return pMap2;
	}
}
