package Zeze.Transaction.Collections;

import Zeze.*;
import Zeze.Transaction.*;
import java.util.*;

public final class PMap1<K, V> extends PMap<K, V> {
	public PMap1(long logKey, tangible.Func1Param<ImmutableDictionary<K, V>, Log> logFactory) {
		super(logKey, logFactory);
	}

	@Override
	public V get(Object objectKey) {
		K key = (K)objectKey;
		return getData()[key];
	}
	@Override
	public void set(K key, V value) {
		if (value == null) {
			throw new NullPointerException();
		}

		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
			LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
			var oldv = tempVar ? log.Value : map;
			var newv = oldv.SetItem(key, value);
			if (newv != oldv) {
				txn.PutLog(NewLog(newv));
				((ChangeNoteMap1<K, V>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteMap1<K, V>(this))).LogPut(key, value);
			}
		}
		else {
			map = map.SetItem(key, value);
		}

	}

	@Override
	public void Add(K key, V value) {
		if (key == null) {
			throw new NullPointerException();
		}
		if (value == null) {
			throw new NullPointerException();
		}

		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
			LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
			var oldv = tempVar ? log.Value : map;
			var newv = oldv.Add(key, value);
			if (newv != oldv) {
				txn.PutLog(NewLog(newv));
				((ChangeNoteMap1<K, V>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteMap1<K, V>(this))).LogPut(key, value);
			}
		}
		else {
			map = map.Add(key, value);
		}
	}

	@Override
	public void AddRange(java.lang.Iterable<Map.Entry<K, V>> pairs) {
		for (var p : pairs) {
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
			boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
			LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
			var oldv = tempVar ? log.Value : map;
			var newv = oldv.AddRange(pairs);
			if (newv != oldv) {
				txn.PutLog(NewLog(newv));
				ChangeNoteMap1<K, V> note = (ChangeNoteMap1<K, V>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteMap1<K, V>(this));
				for (var p : pairs) {
					note.LogPut(p.getKey(), p.getValue());
				}
			}
		}
		else {
			map = map.AddRange(pairs);
		}
	}

	@Override
	public void SetItem(K key, V value) {
		if (key == null) {
			throw new NullPointerException();
		}
		if (value == null) {
			throw new NullPointerException();
		}

		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
			LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
			var oldv = tempVar ? log.Value : map;
			var newv = oldv.SetItem(key, value);
			if (newv != oldv) {
				txn.PutLog(NewLog(newv));
				((ChangeNoteMap1<K, V>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteMap1<K, V>(this))).LogPut(key, value);
			}
		}
		else {
			map = map.SetItem(key, value);
		}
	}

	@Override
	public void SetItems(java.lang.Iterable<Map.Entry<K, V>> pairs) {
		for (var p : pairs) {
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
			boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
			LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
			var oldv = tempVar ? log.Value : map;
			var newv = oldv.SetItems(pairs);
			if (newv != oldv) {
				txn.PutLog(NewLog(newv));
				ChangeNoteMap1<K, V> note = (ChangeNoteMap1<K, V>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteMap1<K, V>(this));
				for (var p : pairs) {
					note.LogPut(p.getKey(), p.getValue());
				}
			}
		}
		else {
			map = map.SetItems(pairs);
		}
	}

	@Override
	public void Add(Map.Entry<K, V> item) {
		if (item.getKey() == null) {
			throw new NullPointerException();
		}
		if (item.getValue() == null) {
			throw new NullPointerException();
		}

		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
			LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
			var oldv = tempVar ? log.Value : map;
			var newv = oldv.Add(item.getKey(), item.getValue());
			if (newv != oldv) {
				txn.PutLog(NewLog(newv));
				((ChangeNoteMap1<K, V>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteMap1<K, V>(this))).LogPut(item.getKey(), item.getValue());
			}
		}
		else {
			map = map.Add(item.getKey(), item.getValue());
		}
	}

	@Override
	public void clear() {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
			LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
			var oldv = tempVar ? log.Value : map;
			if (!oldv.IsEmpty) {
				ChangeNoteMap1<K, V> note = (ChangeNoteMap1<K, V>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteMap1<K, V>(this));
				for (var e : oldv) {
					note.LogRemove(e.Key);
				}
				txn.PutLog(NewLog(ImmutableDictionary<K, V>.Empty));
			}
		}
		else {
			map = ImmutableDictionary<K, V>.Empty;
		}
	}

	@Override
	public boolean Remove(K key) {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
			LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
			var oldv = tempVar ? log.Value : map;
			var newv = oldv.Remove(key);
			if (newv != oldv) {
				txn.PutLog(NewLog(newv));
				((ChangeNoteMap1<K, V>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteMap1<K, V>(this))).LogRemove(key);
				return true;
			}
			else {
				return false;
			}
		}
		else {
			var old = map;
			map = map.Remove(key);
			return old != map;
		}
	}

	@Override
	public boolean Remove(Map.Entry<K, V> item) {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
			LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
			var oldv = tempVar ? log.Value : map;
			// equals 处有box，能否优化掉？
			Object olde;
//C# TO JAVA CONVERTER TODO TASK: The following method call contained an unresolved 'out' keyword - these cannot be converted using the 'OutObject' helper class unless the method is within the code being modified:
			if (oldv.TryGetValue(item.getKey(), out olde) && olde.equals(item.getValue())) {
				var newv = oldv.Remove(item.getKey());
				txn.PutLog(NewLog(newv));
				((ChangeNoteMap1<K, V>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteMap1<K, V>(this))).LogRemove(item.getKey());
				return true;
			}
			else {
				return false;
			}
		}
		else {
			// equals处有box
			Object oldv;
//C# TO JAVA CONVERTER TODO TASK: The following method call contained an unresolved 'out' keyword - these cannot be converted using the 'OutObject' helper class unless the method is within the code being modified:
			if (map.TryGetValue(item.getKey(), out oldv) && oldv.equals(item.getValue())) {
				map = map.Remove(item.getKey());
				return true;
			}
			else {
				return false;
			}
		}
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo tableKey) {

	}
}