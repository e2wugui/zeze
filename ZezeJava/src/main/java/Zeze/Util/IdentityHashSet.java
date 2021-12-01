package Zeze.Util;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

public class IdentityHashSet<E> implements Iterable<E> {
    private final Map<E, E> Impl;

    public IdentityHashSet() {
        Impl = Collections.synchronizedMap(new IdentityHashMap<>());
    }

    public boolean Add(E e) {
        return Impl.putIfAbsent(e, e) == null;
    }

    public boolean Remove(E e) {
        return Impl.remove(e) != null;
    }


    @Override
    public Iterator<E> iterator() {
        return Impl.keySet().iterator();
    }
}
