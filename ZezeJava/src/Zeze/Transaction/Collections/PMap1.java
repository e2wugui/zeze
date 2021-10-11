package Zeze.Transaction.Collections;

import Zeze.Transaction.*;
import java.util.*;
import org.pcollections.Empty;

public final class PMap1<K, V> extends PMap<K, V> {
	public PMap1(long logKey, LogFactory<org.pcollections.PMap<K, V>> logFactory) {
		super(logKey, logFactory);
	}

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
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldm = null != log ? ((LogV)log).Value : map;
			var oldv = oldm.get(key);
			if (oldv != value) {
				var newm = oldm.plus(key, value);
				txn.PutLog(NewLog(newm));
				((ChangeNoteMap1<K, V>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteMap1<K, V>(this))).LogPut(key, value);
			}
			return oldv;
		}
		else {
			var oldv = map.get(key);
			map = map.plus(key, value);
			return oldv;
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
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldm = null != log ? ((LogV)log).Value : map;
			var newm = oldm.plusAll(m);
			if (newm != oldm) {
				txn.PutLog(NewLog(newm));
				ChangeNoteMap1<K, V> note = (ChangeNoteMap1<K, V>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteMap1<K, V>(this));
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
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldm = null != log ? ((LogV)log).Value : map;
			if (!oldm.isEmpty()) {
				ChangeNoteMap1<K, V> note = (ChangeNoteMap1<K, V>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteMap1<K, V>(this));
				for (var e : oldm.entrySet()) {
					note.LogRemove(e.getKey());
				}
				txn.PutLog(NewLog(Empty.map()));
			}
		}
		else {
			map = Empty.map();
		}
	}

	@Override
	public boolean remove(K key) {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldm = null != log ? ((LogV)log).Value : map;
			var newm = oldm.minus(key);
			if (newm != oldm) {
				txn.PutLog(NewLog(newm));
				((ChangeNoteMap1<K, V>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteMap1<K, V>(this))).LogRemove(key);
				return true;
			}
			else {
				return false;
			}
		}
		else {
			var old = map;
			map = map.minus(key);
			return old != map;
		}
	}

	@Override
	public boolean remove(Map.Entry<K, V> item) {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldm = null != log ? ((LogV)log).Value : map;
			// equals 处有box，能否优化掉？
			Object olde = oldm.get(item.getKey());
			if (null == olde)
				return false;

			if (olde.equals(item.getValue())) {
				var newm = oldm.minus(item.getKey());
				txn.PutLog(NewLog(newm));
				((ChangeNoteMap1<K, V>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteMap1<K, V>(this))).LogRemove(item.getKey());
				return true;
			}
			else {
				return false;
			}
		}
		else {
			// equals处有box
			Object oldv = map.get(item.getKey());
			if (null == oldv)
				return false;
			if (oldv.equals(item.getValue())) {
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