package Zeze.Transaction.Collections;

import java.util.Collection;
import org.pcollections.Empty;
import Zeze.Transaction.*;

public final class PSet1<E> extends PSet<E> {
	public PSet1(long logKey, LogFactory<org.pcollections.PSet<E>> logFactory) {
		super(logKey, logFactory);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean add(E item) {
		if (item == null) {
			throw new NullPointerException();
		}

		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			var olds = null != log ? ((LogV<E>)log).Value : set;
			var news = olds.plus(item);
			if (news != olds) {
				txn.PutLog(NewLog(news));
				((ChangeNoteSet<E>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteSet<E>(this))).LogAdd(item);
				return true;
			}
			else {
				return false;
			}
		}
		else {
			var olds = set;
			set = set.plus(item);
			return olds != set;
		}
	}

	@Override
	public void clear() {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var olds = null != log ? ((LogV<E>)log).Value : set;
			if (!olds.isEmpty()) {
				txn.PutLog(NewLog(Empty.set()));
				@SuppressWarnings("unchecked")
				ChangeNoteSet<E> note = (ChangeNoteSet<E>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteSet<E>(this));
				for (var item : olds) {
					note.LogRemove(item);
				}
			}
		}
		else {
			set = Empty.set();
		}
	}


	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(Object item) {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			var olds = null != log ? ((LogV<E>)log).Value : set;
			var news = olds.minus(item);
			if (news != olds) {
				txn.PutLog(NewLog(news));
				((ChangeNoteSet<E>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteSet<E>(this))).LogRemove((E)item);
				return true;
			}

			return false;
		}
		else {
			var old = set;
			set = set.minus(item);
			return old != set;
		}
	}

	@Override
	protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo tableKey) {
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var olds = null != log ? ((LogV<E>)log).Value : set;
			var news = olds.plusAll(c);
			if (news != olds) {
				txn.PutLog(NewLog(news));
				@SuppressWarnings("unchecked")
				var note = ((ChangeNoteSet<E>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteSet<E>(this)));
                for (var item : c) {
                    if (false == olds.contains(item))
                        note.LogAdd(item);
                }
			}
		}
		else {
			set = set.plusAll(c);
		}
		return true;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var olds = null != log ? ((LogV<E>)log).Value : set;
			var news = olds.minusAll(c);
			if (news != olds) {
				txn.PutLog(NewLog(news));
				@SuppressWarnings("unchecked")
				var note = ((ChangeNoteSet<E>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteSet<E>(this)));
                for (var item : c) {
                    note.LogRemove((E)item);
                }
                return true;
			}
			return false;
		}
		else {
			var olds = set;
			set = set.minusAll(c);
			return olds != set;
		}
	}

}