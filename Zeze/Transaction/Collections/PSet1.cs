using System;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Linq;

namespace Zeze.Transaction.Collections
{
    public sealed class PSet1<E> : PSet<E>
    {
        public PSet1(long logKey, Func<ImmutableHashSet<E>, Log> logFactory) : base(logKey, logFactory)
        {
        }

        public override bool Add(E item)
        {
            if (item == null)
                throw new ArgumentNullException();

            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetLog(LogKey) is LogV log ? log.Value : set;
                var newv = oldv.Add(item);
                if (newv != oldv)
                {
                    txn.PutLog(NewLog(newv));
                    ((ChangeNoteSet<E>)txn.GetOrAddChangeNote(this.ObjectId, () => new ChangeNoteSet<E>(this))).LogAdd(item);
                    return true;
                }
                else
                {
                    return false;
                }
            }
            else
            {
                set = set.Add(item);
                return true;
            }
        }

        public override void Clear()
        {
            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetLog(LogKey) is LogV log ? log.Value : set;
                if (!oldv.IsEmpty)
                {
                    txn.PutLog(NewLog(ImmutableHashSet<E>.Empty));
                    ChangeNoteSet<E> note = (ChangeNoteSet<E>)txn.GetOrAddChangeNote(this.ObjectId, () => new ChangeNoteSet<E>(this));
                    foreach (var item in oldv)
                        note.LogRemove(item);
                }
            }
            else
            {
                set = ImmutableHashSet<E>.Empty;
            }
        }



        public override void ExceptWith(IEnumerable<E> other)
        {
            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetLog(LogKey) is LogV log ? log.Value : set;
                var newv = oldv.Except(other);
                if (newv != oldv)
                {
                    txn.PutLog(NewLog(newv));
                    ChangeNoteSet<E> note = (ChangeNoteSet<E>)txn.GetOrAddChangeNote(this.ObjectId, () => new ChangeNoteSet<E>(this));
                    foreach (var item in other)
                        note.LogRemove(item);
                }
            }
            else
            {
                set = set.Except(other);
            }
        }


        public override void IntersectWith(IEnumerable<E> other)
        {
            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetLog(LogKey) is LogV log ? log.Value : set;
                var newv = oldv.Intersect(other);
                if (newv != oldv)
                {
                    txn.PutLog(NewLog(newv));
                    ChangeNoteSet<E> note = (ChangeNoteSet<E>)txn.GetOrAddChangeNote(this.ObjectId, () => new ChangeNoteSet<E>(this));
                    foreach (var old in oldv)
                    {
                        if (false == other.Contains(old))
                            note.LogRemove(old);
                    }
                }
            }
            else
            {
                set = set.Intersect(other);
            }
        }

        public override bool Remove(E item)
        {
            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetLog(LogKey) is LogV log ? log.Value : set;
                var newv = oldv.Remove(item);
                if (newv != oldv)
                {
                    txn.PutLog(NewLog(newv));
                    ((ChangeNoteSet<E>)txn.GetOrAddChangeNote(this.ObjectId, () => new ChangeNoteSet<E>(this))).LogRemove(item);
                    return true;
                }
                else
                {
                    return false;
                }
            }
            else
            {
                var old = set;
                set = set.Remove(item);
                return old != set;
            }
        }

        public override void SymmetricExceptWith(IEnumerable<E> other)
        {
            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetLog(LogKey) is LogV log ? log.Value : set;
                var newv = oldv.SymmetricExcept(other);
                if (newv != oldv)
                {
                    txn.PutLog(NewLog(newv));
                    ChangeNoteSet<E> note = (ChangeNoteSet<E>)txn.GetOrAddChangeNote(this.ObjectId, () => new ChangeNoteSet<E>(this));
                    // this: 1,2 other: 2,3 result: 1,3
                    foreach (var item in other)
                    {
                        if (oldv.Contains(item))
                        {
                            note.LogRemove(item);
                        }
                        else
                        {
                            note.LogAdd(item);
                        }
                    }
                }
            }
            else
            {
                set = set.SymmetricExcept(other);
            }
        }

        public override void UnionWith(IEnumerable<E> other)
        {
            foreach (E v in other)
            {
                if (null == v)
                    throw new ArgumentNullException();
            }

            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetLog(LogKey) is LogV log ? log.Value : set;
                var newv = oldv.Union(other);
                if (newv != oldv)
                {
                    txn.PutLog(NewLog(newv));
                    ChangeNoteSet<E> note = (ChangeNoteSet<E>)txn.GetOrAddChangeNote(this.ObjectId, () => new ChangeNoteSet<E>(this));
                    foreach (var item in other)
                    {
                        if (false == oldv.Contains(item))
                            note.LogAdd(item);
                    }
                }
            }
            else
            {
                set = set.Union(other);
            }
        }

        protected override void InitChildrenRootInfo(Record.RootInfo tableKey)
        {
        }
    }
}
