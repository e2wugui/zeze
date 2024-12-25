package Zeze.Transaction.GTable;

import java.io.Serializable;
import java.util.Map;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.PMap2;
import com.google.common.base.Supplier;

public class GTable2<R extends Comparable<R>, C extends Comparable<C>, V extends Bean, VReadOnly>
		extends StandardTable<R, C, V> {
	private final PMap2<R, BeanMap2<C, V, VReadOnly>> pMap2;

	@SuppressWarnings({"rawtypes", "unchecked"})
	public GTable2(Class<R> rowClass, Class<C> colClass, Class<V> valClass) {
		this.pMap2 = new PMap2(rowClass, BeanMap2.class);
		PMap2 raw = pMap2;
		super.backingMap = raw;
		super.factory = new Factory<>(colClass, valClass);
	}

	public PMap2<R, BeanMap2<C, V, VReadOnly>> getPMap2() {
		return pMap2;
	}

	private static class Factory<C extends Comparable<C>, V extends Bean> implements Supplier<Map<C, V>>, Serializable {
		private final Class<C> colClass;
		private final Class<V> valClass;
		private static final long serialVersionUID = 0L;

		Factory(Class<C> colClass, Class<V> valueClass) {
			this.colClass = colClass;
			this.valClass = valueClass;
		}

		@Override
		public Map<C, V> get() {
			return new BeanMap2<>(colClass, valClass);
		}
	}
}
