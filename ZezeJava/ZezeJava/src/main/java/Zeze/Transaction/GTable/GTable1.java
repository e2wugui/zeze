package Zeze.Transaction.GTable;

import java.io.Serializable;
import java.util.Map;
import Zeze.Transaction.Collections.PMap2;
import com.google.common.base.Supplier;

public class GTable1<R extends Comparable<R>, C extends Comparable<C>, V> extends StandardTable<R, C, V> {
	@SuppressWarnings({"unchecked", "rawtypes"})
	public GTable1(Class<R> rowClass, Class<C> colClass, Class<V> valueClass) {
		super(new PMap2(rowClass, BeanMap1.class), new Factory<>(colClass, valueClass));
	}

	private static class Factory<C extends Comparable<C>, V> implements Supplier<Map<C, V>>, Serializable {
		private final Class<C> colClass;
		private final Class<V> valClass;
		private static final long serialVersionUID = 0L;

		Factory(Class<C> colClass, Class<V> valueClass) {
			this.colClass = colClass;
			this.valClass = valueClass;
		}

		@Override
		public Map<C, V> get() {
			return new BeanMap1<>(colClass, valClass);
		}
	}
}
