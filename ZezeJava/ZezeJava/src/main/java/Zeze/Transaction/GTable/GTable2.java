package Zeze.Transaction.GTable;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.PMap2;
import Zeze.Util.Factory;
import com.google.common.base.Supplier;

public class GTable2<R extends Comparable<R>, C extends Comparable<C>, V extends Bean, VReadOnly>
		extends StandardTable<R, C, V> {
	public GTable2(Class<R> rowClass, Class<C> colClass, Class<V> valClass) {
		// TODO 这里怎么传递参数啊。
		//super(new PMap2<R, BeanMap2<C, V, VReadOnly>>(rowClass, BeanMap2.class), new Factory<>(colClass, valClass));
		super(new LinkedHashMap<>(), new Factory<>(colClass, valClass)); // TODO 先编译通过。
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
