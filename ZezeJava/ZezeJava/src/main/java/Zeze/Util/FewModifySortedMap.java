package Zeze.Util;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

public class FewModifySortedMap<K, V> extends FewModifyMap<K, V> implements SortedMap<K, V> {
	public FewModifySortedMap() {
		super(true);
	}

	private SortedMap<K, V> prepareSortedMap() {
		return (SortedMap<K, V>)prepareRead();
	}

	@Override
	public Comparator<? super K> comparator() {
		return prepareSortedMap().comparator();
	}

	@NotNull
	@Override
	public SortedMap<K, V> subMap(K fromKey, K toKey) {
		return prepareSortedMap().subMap(fromKey, toKey);
	}

	@NotNull
	@Override
	public SortedMap<K, V> headMap(K toKey) {
		return prepareSortedMap().headMap(toKey);
	}

	@NotNull
	@Override
	public SortedMap<K, V> tailMap(K fromKey) {
		return prepareSortedMap().tailMap(fromKey);
	}

	@Override
	public K firstKey() {
		return prepareSortedMap().firstKey();
	}

	@Override
	public K lastKey() {
		return prepareSortedMap().lastKey();
	}
}
