// auto-generated
package Zezex.Provider;

import Zeze.Serialize.*;

public final class BModule extends Zeze.Transaction.Bean implements BModuleReadOnly {
    public static final int ChoiceTypeDefault = 0;
    public static final int ChoiceTypeHashAccount = 1;
    public static final int ChoiceTypeHashRoleId = 2;
    public static final int ConfigTypeDefault = 0;
    public static final int ConfigTypeSpecial = 1;
    public static final int ConfigTypeDynamic = 2;

    private int _ChoiceType;
    private int _ConfigType;

    public int getChoiceType(){
        if (false == this.isManaged())
            return _ChoiceType;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _ChoiceType;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ChoiceType)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _ChoiceType;
    }

    public void setChoiceType(int value){
        if (false == this.isManaged()) {
            _ChoiceType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ChoiceType(this, value));
    }

    public int getConfigType(){
        if (false == this.isManaged())
            return _ConfigType;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _ConfigType;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ConfigType)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _ConfigType;
    }

    public void setConfigType(int value){
        if (false == this.isManaged()) {
            _ConfigType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ConfigType(this, value));
    }


    public BModule() {
         this(0);
    }

    public BModule(int _varId_) {
        super(_varId_);
    }

    public void Assign(BModule other) {
        setChoiceType(other.getChoiceType());
        setConfigType(other.getConfigType());
    }

    public BModule CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BModule Copy() {
        var copy = new BModule();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BModule a, BModule b) {
        BModule save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = 8897491527272666170L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final static class Log__ChoiceType extends Zeze.Transaction.Log1<BModule, Integer> {
        public Log__ChoiceType(BModule self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._ChoiceType = this.getValue(); }
    }

    private final static class Log__ConfigType extends Zeze.Transaction.Log1<BModule, Integer> {
        public Log__ConfigType(BModule self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._ConfigType = this.getValue(); }
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
        sb.append(" ".repeat(level * 4)).append("Zezex.Provider.BModule: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("ChoiceType").append("=").append(getChoiceType()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("ConfigType").append("=").append(getConfigType()).append("").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(2); // Variables.Count
        _os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getChoiceType());
        _os_.WriteInt(ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getConfigType());
    }

    @Override
    public void Decode(ByteBuffer _os_) {
        for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
            int _tagid_ = _os_.ReadInt();
            switch (_tagid_) {
                case (ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT): 
                    setChoiceType(_os_.ReadInt());
                    break;
                case (ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT): 
                    setConfigType(_os_.ReadInt());
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
        if (getChoiceType() < 0) return true;
        if (getConfigType() < 0) return true;
        return false;
    }

}
