package Zeze.Transaction.Collections;

import java.util.function.Consumer;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Locks;
import Zeze.Transaction.Map2TestSupport;
import Zeze.Transaction.Transaction;

/** Source write-intent fixture with a real final savepoint and the production change-export hook. */
public final class Map2DeltaTestSupport {
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static LogBean decodeSorted(LogBean template, Zeze.Serialize.IByteBuffer input) {
		if (template instanceof LogSortedMap2 map)
			return LogSortedMap2.decode(map.getVariableId(), map.meta, map.comparator(), input);
		var map = (LogSortedMap1)template;
		var comparator = ((java.util.SortedMap)map.getReplaced()).comparator();
		return LogSortedMap1.decode(map.getVariableId(), map.meta, comparator, input);
	}

	private Map2DeltaTestSupport() {
	}

	/** 为比较器测试初始化底层状态，无须公开具体的事务日志子类。 */
	public static <K extends Comparable<K>, V> void initSortedMap(
			PSortedMap<K, V> source, org.pcollections.PSortedMap<K, V> value) {
		new LogSortedMap<>(source, value).commit();
	}

	public static <K, V extends Bean> LogMap2<K, V> map(
		PMap2<K, V> map, Consumer<LogMap<K, V>> edit) {
		Map2TestSupport.attach(map);
		var transaction = Transaction.create(new Locks());
		transaction.begin();
		try {
			var delta = new LogMapDelta.Map2<K, V>(map, map.getMap());
			transaction.putLog(delta);
			edit.accept(delta);
			delta.commit();
			return (LogMap2<K, V>)map.createLogBean(delta);
		} finally {
			Transaction.destroy();
		}
	}

	public static <K extends Comparable<K>, V extends Bean> LogSortedMap2<K, V> sorted(
		PSortedMap2<K, V> map, Consumer<LogSortedMap<K, V>> edit) {
		Map2TestSupport.attach(map);
		var transaction = Transaction.create(new Locks());
		transaction.begin();
		try {
			var delta = new LogSortedMapDelta.Map2<K, V>(map, map.getMap());
			transaction.putLog(delta);
			edit.accept(delta);
			delta.commit();
			return (LogSortedMap2<K, V>)map.createLogBean(delta);
		} finally {
			Transaction.destroy();
		}
	}
}
