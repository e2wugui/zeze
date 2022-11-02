using Org.BouncyCastle.Asn1.Mozilla;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Transaction.Collections
{
    public class CollOne<V> : Collection where V : Bean, new()
    {
        internal V _Value = new();

        public V Value
        {
            get
            {
                if (!IsManaged)
                    return _Value;

                var txn = Transaction.Current;
                if (null == txn)
                    return _Value;

                var log = (LogOne<V>)txn.GetLog(Parent.ObjectId + VariableId);
                if (null == log)
                    return _Value;

                return log.Value;
            }

            set
            {
                if (null == value)
                    throw new ArgumentNullException(nameof(value));

                if (IsManaged)
                {
                    value.InitRootInfo(RootInfo, this);
                    var txn = Transaction.Current;
                    txn.VerifyRecordAccessed(this);
                    var log = (LogOne<V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
                    log.SetValue(value);
                }
                else
                {
                    _Value = value;
                }
            }
        }

        public void Assign(CollOne<V> other)
        {
            Value = (V)other.Value.Copy();
        }

        public override bool Equals(object obj)
        {
            if (obj == this)
                return true;
            if (obj is CollOne<V> other)
                return Value.Equals(other.Value);
            return false;
        }

        public override int GetHashCode()
        {
            return Value.GetHashCode();
        }

        public override LogBean CreateLogBean()
        {
            return new LogOne<V>()
            {
                Belong = Parent,
                This = this,
                VariableId = VariableId,
                Value = Value,
            };
        }

        public override void Decode(ByteBuffer bb)
        {
            if (null == Value)
                Value = new V();
            Value.Decode(bb);
        }

        public override void Encode(ByteBuffer bb)
        {
            Value.Encode(bb);
        }

        protected override void InitChildrenRootInfo(Record.RootInfo root)
        {
            Value.InitRootInfo(root, this);
        }

        protected override void ResetChildrenRootInfo()
        {
            Value.ResetRootInfo();
        }

        public override void FollowerApply(Log _log)
        {
            var log = (LogOne<V>)_log;
            if (null != log.Value)
            {
                _Value = log.Value;
            }
            else if (null != log.LogBean)
            {
                _Value.FollowerApply(log.LogBean);
            }
        }

        public override CollOne<V> Copy()
        { 
            var copy = new CollOne<V>();
            copy._Value = (V)Value.Copy();
            return copy;
        }
    }
}
