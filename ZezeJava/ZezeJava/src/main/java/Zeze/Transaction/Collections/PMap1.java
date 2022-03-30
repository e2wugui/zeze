package Zeze.Transaction.Collections;

import Zeze.Transaction.*;
import java.util.*;
import org.pcollections.Empty;

public final class PMap1<K, V> extends PMap<K, V> {
	public PMap1(long logKey, LogFactory<org.pcollections.PMap<K, V>> logFactory) {
		super(logKey, logFactory);
	}

	@SuppressWarnings("unchecked")
	@Override
	public V put(K key, V value) {
		if (key == null) {
			throw new NullPointerException();
		}
		if (value == null) {
			throw new NullPointerException();
		}

		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			assert txn != null;
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			var oldM = null != log ? ((LogV<K, V>)log).Value : map;
			var oldV = oldM.get(key);
			if (oldV != value) {
				var newM = oldM.plus(key, value);
				txn.PutLog(NewLog(newM));
				((ChangeNoteMap1<K, V>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteMap1<>(this))).LogPut(key, value);
			}
			return oldV;
		}
		else {
			var oldV = map.get(key);
			map = map.plus(key, value);
			return oldV;
		}
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (var p : m.entrySet()) {
			if (p.getKey() == null) {
				throw new NullPointerException();
			}
			if (p.getValue() == null) {
				throw new NullPointerException();
			}
		}

		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			assert txn != null;
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldM = null != log ? ((LogV<K, V>)log).Value : map;
			var newM = oldM.plusAll(m);
			if (newM != oldM) {
				txn.PutLog(NewLog(newM));
				@SuppressWarnings("unchecked")
				ChangeNoteMap1<K, V> note = (ChangeNoteMap1<K, V>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteMap1<>(this));
				for (var p : m.entrySet()) {
					note.LogPut(p.getKey(), p.getValue());
				}
			}
		}
		else {
			map = map.plusAll(m);
		}
	}

	@Override
	public void clear() {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			assert txn != null;
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldM = null != log ? ((LogV<K, V>)log).Value : map;
			if (!oldM.isEmpty()) {
				@SuppressWarnings("unchecked")
				ChangeNoteMap1<K, V> note = (ChangeNoteMap1<K, V>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteMap1<>(this));
				for (var e : oldM.entrySet()) {
					note.LogRemove(e.getKey());
				}
				txn.PutLog(NewLog(Empty.map()));
			}
		}
		else {
			map = Empty.map();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public V remove(Object key) {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			assert txn != null;
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			var oldM = null != log ? ((LogV<K, V>)log).Value : map;
			var newM = oldM.minus(key);
			//noinspection SuspiciousMethodCalls
			var exist = oldM.get(key);
			if (newM != oldM) {
				txn.PutLog(NewLog(newM));
				((ChangeNoteMap1<K, V>)txn.GetOrAddChangeNote(this.getObjectId(),
						() -> new ChangeNoteMap1<>(this))).LogRemove((K)key);
			}
			return exist;
		}
		else {
			var old = map;
			//noinspection SuspiciousMethodCalls
			var exist = old.get(key);
			map = map.minus(key);
			return exist;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(Map.Entry<K, V> item) {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			assert txn != null;
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			var oldM = null != log ? ((LogV<K, V>)log).Value : map;
			// equals 处有box，能否优化掉？
			Object oldE = oldM.get(item.getKey());
			if (null == oldE)
				return false;

			if (oldE.equals(item.getValue())) {
				var newM = oldM.minus(item.getKey());
				txn.PutLog(NewLog(newM));
				((ChangeNoteMap1<K, V>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteMap1<>(this))).LogRemove(item.getKey());
				return true;
			}
			else {
				return false;
			}
		}
		else {
			// equals处有box
			Object oldV = map.get(item.getKey());
			if (null == oldV)
				return false;
			if (oldV.equals(item.getValue())) {
				map = map.minus(item.getKey());
				return true;
			}
			else {
				return false;
			}
		}
	}

	@Override
	protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo tableKey) {

	}
}
