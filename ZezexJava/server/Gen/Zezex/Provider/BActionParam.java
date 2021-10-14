// auto-generated
package Zezex.Provider;

import Zeze.Serialize.*;

public final class BActionParam extends Zeze.Transaction.Bean implements BActionParamReadOnly {
    private String _Name;
    private Zeze.Net.Binary _Params;

    public String getName(){
        if (false == this.isManaged())
            return _Name;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _Name;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Name)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _Name;
    }

    public void setName(String value){
            if (null == value)
                throw new IllegalArgumentException();
        if (false == this.isManaged()) {
            _Name = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Name(this, value));
    }

    public Zeze.Net.Binary getParams(){
        if (false == this.isManaged())
            return _Params;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _Params;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Params)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _Params;
    }

    public void setParams(Zeze.Net.Binary value){
            if (null == value)
                throw new IllegalArgumentException();
        if (false == this.isManaged()) {
            _Params = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Params(this, value));
    }


    public BActionParam() {
         this(0);
    }

    public BActionParam(int _varId_) {
        super(_varId_);
        _Name = "";
        _Params = Zeze.Net.Binary.Empty;
    }

    public void Assign(BActionParam other) {
        setName(other.getName());
        setParams(other.getParams());
    }

    public BActionParam CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BActionParam Copy() {
        var copy = new BActionParam();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BActionParam a, BActionParam b) {
        BActionParam save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = 9109359638064175139L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final static class Log__Name extends Zeze.Transaction.Log1<BActionParam, String> {
        public Log__Name(BActionParam self, String value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._Name = this.getValue(); }
    }

    private final static class Log__Params extends Zeze.Transaction.Log1<BActionParam, Zeze.Net.Binary> {
        public Log__Params(BActionParam self, Zeze.Net.Binary value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._Params = this.getValue(); }
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
        sb.append(" ".repeat(level * 4)).append("Zezex.Provider.BActionParam: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("Name").append("=").append(getName()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("Params").append("=").append(getParams()).append("").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(2); // Variables.Count
        _os_.WriteInt(ByteBuffer.STRING | 1 << ByteBuffer.TAG_SHIFT);
        _os_.WriteString(getName());
        _os_.WriteInt(ByteBuffer.BYTES | 2 << ByteBuffer.TAG_SHIFT);
        _os_.WriteBinary(getParams());
    }

    @Override
    public void Decode(ByteBuffer _os_) {
        for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
            int _tagid_ = _os_.ReadInt();
            switch (_tagid_) {
                case (ByteBuffer.STRING | 1 << ByteBuffer.TAG_SHIFT): 
                    setName(_os_.ReadString());
                    break;
                case (ByteBuffer.BYTES | 2 << ByteBuffer.TAG_SHIFT): 
                    setParams(_os_.ReadBinary());
                    break;
                default:
                    ByteBuffer.SkipUnknownField(_tagid_, _os_);
                    break;
            }
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    public boolean NegativeCheck() {
        return false;
    }

}
