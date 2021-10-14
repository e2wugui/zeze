// auto-generated
package Game.Login;

import Zeze.Serialize.*;

public final class BReliableNotify extends Zeze.Transaction.Bean implements BReliableNotifyReadOnly {
    private Zeze.Transaction.Collections.PList1<Zeze.Net.Binary> _Notifies; // full encoded protocol list
    private long _ReliableNotifyTotalCountStart; // Notify的计数开始。客户端收到的总计数为：start + Notifies.Count

    public Zeze.Transaction.Collections.PList1<Zeze.Net.Binary> getNotifies() {
        return _Notifies;
    }


    public long getReliableNotifyTotalCountStart(){
        if (false == this.isManaged())
            return _ReliableNotifyTotalCountStart;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _ReliableNotifyTotalCountStart;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ReliableNotifyTotalCountStart)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _ReliableNotifyTotalCountStart;
    }

    public void setReliableNotifyTotalCountStart(long value){
        if (false == this.isManaged()) {
            _ReliableNotifyTotalCountStart = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ReliableNotifyTotalCountStart(this, value));
    }


    public BReliableNotify() {
         this(0);
    }

    public BReliableNotify(int _varId_) {
        super(_varId_);
        _Notifies = new Zeze.Transaction.Collections.PList1<Zeze.Net.Binary>(getObjectId() + 1, (_v) -> new Log__Notifies(this, _v));
    }

    public void Assign(BReliableNotify other) {
        getNotifies().clear();
        for (var e : other.getNotifies()) {
            getNotifies().add(e);
        }
        setReliableNotifyTotalCountStart(other.getReliableNotifyTotalCountStart());
    }

    public BReliableNotify CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BReliableNotify Copy() {
        var copy = new BReliableNotify();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BReliableNotify a, BReliableNotify b) {
        BReliableNotify save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = 2795526929721142563L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final class Log__Notifies extends Zeze.Transaction.Collections.PList.LogV<Zeze.Net.Binary> {
        public Log__Notifies(BReliableNotify host, org.pcollections.PVector<Zeze.Net.Binary> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 1; }
        public BReliableNotify getBeanTyped() { return (BReliableNotify)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._Notifies); }
    }

    private final static class Log__ReliableNotifyTotalCountStart extends Zeze.Transaction.Log1<BReliableNotify, Long> {
        public Log__ReliableNotifyTotalCountStart(BReliableNotify self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._ReliableNotifyTotalCountStart = this.getValue(); }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    @Override
    public void BuildString(StringBuilder sb, int level) {
        sb.append(" ".repeat(level * 4)).append("Game.Login.BReliableNotify: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("Notifies").append("=[").append(System.lineSeparator());
        level++;
        for (var _item_ : getNotifies()) {
            sb.append(" ".repeat(level * 4)).append("Item").append("=").append(_item_).append(",").append(System.lineSeparator());
        }
        level--;
        sb.append(" ".repeat(level * 4)).append("],").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("ReliableNotifyTotalCountStart").append("=").append(getReliableNotifyTotalCountStart()).append("").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(2); // Variables.Count
        _os_.WriteInt(ByteBuffer.LIST | 1 << ByteBuffer.TAG_SHIFT);
        {
            var _state_ = _os_.BeginWriteSegment();
            _os_.WriteInt(ByteBuffer.BYTES);
            _os_.WriteInt(getNotifies().size());
            for (var _v_ : getNotifies()) {
                _os_.WriteBinary(_v_);
            }
            _os_.EndWriteSegment(_state_); 
        }
        _os_.WriteInt(ByteBuffer.LONG | 2 << ByteBuffer.TAG_SHIFT);
        _os_.WriteLong(getReliableNotifyTotalCountStart());
    }

    @Override
    public void Decode(ByteBuffer _os_) {
        for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
            int _tagid_ = _os_.ReadInt();
            switch (_tagid_) {
                case (ByteBuffer.LIST | 1 << ByteBuffer.TAG_SHIFT):
                    {
                        var _state_ = _os_.BeginReadSegment();
                        _os_.ReadInt(); // skip collection.value typetag
                        getNotifies().clear();
                        for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_) {
                            Zeze.Net.Binary _v_;
                            _v_ = _os_.ReadBinary();
                            getNotifies().add(_v_);
                        }
                        _os_.EndReadSegment(_state_);
                    }
                    break;
                case (ByteBuffer.LONG | 2 << ByteBuffer.TAG_SHIFT): 
                    setReliableNotifyTotalCountStart(_os_.ReadLong());
                    break;
                default:
                    ByteBuffer.SkipUnknownField(_tagid_, _os_);
                    break;
            }
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Notifies.InitRootInfo(root, this);
    }

    @Override
    public boolean NegativeCheck() {
        if (getReliableNotifyTotalCountStart() < 0) return true;
        return false;
    }

}
