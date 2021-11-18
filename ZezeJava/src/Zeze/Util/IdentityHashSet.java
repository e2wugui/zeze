package Zeze.Util;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IdentityHashSet<E> implements Iterable<E> {
    private Map<E, E> Impl;

    public IdentityHashSet() {
        Impl = Collections.synchronizedMap(new IdentityHashMap<E, E>());
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
