package Zeze.Transaction.GTable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.Meta2;
import Zeze.Transaction.Collections.PMap2;
import com.google.common.base.Supplier;
import org.jetbrains.annotations.NotNull;

public class GTable2<R, C, V extends Bean, VReadOnly> extends StandardTable<R, C, V> {
	private static final class Factory<R, C, V extends Bean, VReadOnly> implements Supplier<Map<C, V>> {
		private final @NotNull Meta2<R, BeanMap2<C, V, VReadOnly>> pmapMeta;
		private final @NotNull Meta2<C, V> bmapMeta;

		Factory(@NotNull Meta2<R, BeanMap2<C, V, VReadOnly>> pmapMeta, @NotNull Meta2<C, V> bmapMeta) {
			this.pmapMeta = pmapMeta;
			this.bmapMeta = bmapMeta;
		}

		@Override
		public @NotNull Map<C, V> get() {
			return new BeanMap2<>(bmapMeta);
		}
	}

	private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, Factory<?, ?, ?, ?>>>>
			factories = new ConcurrentHashMap<>();

	private final @NotNull PMap2<R, BeanMap2<C, V, VReadOnly>> pMap2;

	public void variableId(int id) {
		pMap2.variableId(id);
	}

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

	@SuppressWarnings("unchecked")
	public GTable2(@NotNull Class<R> rowClass, @NotNull Class<C> colClass, @NotNull Class<V> valClass) {
		var factory = getFactory(rowClass, colClass, valClass);
		this.pMap2 = new PMap2<>((Meta2<R, BeanMap2<C, V, VReadOnly>>)(Meta2<?, ?>)factory.pmapMeta);
		super.backingMap = (Map<R, Map<C, V>>)(Map<?, ?>)pMap2;
		super.factory = factory;
	}

	@SuppressWarnings("unchecked")
	private static <R, C, V extends Bean, VReadOnly> @NotNull Factory<R, C, V, VReadOnly> getFactory(
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
