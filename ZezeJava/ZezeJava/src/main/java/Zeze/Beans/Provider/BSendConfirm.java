// auto-generated @formatter:off
package Zeze.Beans.Provider;

import Zeze.Serialize.ByteBuffer;

public final class BSendConfirm extends Zeze.Transaction.Bean {
    private long _ConfirmSerialId; // SendConfirm 参数，即Send.Argument.ConfirmSerialId
    private String _LinkName;

    public long getConfirmSerialId() {
        if (!isManaged())
            return _ConfirmSerialId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ConfirmSerialId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ConfirmSerialId)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _ConfirmSerialId;
    }

    public void setConfirmSerialId(long value) {
        if (!isManaged()) {
            _ConfirmSerialId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ConfirmSerialId(this, value));
    }

    public String getLinkName() {
        if (!isManaged())
            return _LinkName;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _LinkName;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__LinkName)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _LinkName;
    }

    public void setLinkName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _LinkName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__LinkName(this, value));
    }

    public BSendConfirm() {
         this(0);
    }

    public BSendConfirm(int _varId_) {
        super(_varId_);
        _LinkName = "";
    }

    public void Assign(BSendConfirm other) {
        setConfirmSerialId(other.getConfirmSerialId());
        setLinkName(other.getLinkName());
    }

    public BSendConfirm CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BSendConfirm Copy() {
        var copy = new BSendConfirm();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BSendConfirm a, BSendConfirm b) {
        BSendConfirm save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 4100720956573778471L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__ConfirmSerialId extends Zeze.Transaction.Log1<BSendConfirm, Long> {
        public Log__ConfirmSerialId(BSendConfirm self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._ConfirmSerialId = this.getValue(); }
    }

    private static final class Log__LinkName extends Zeze.Transaction.Log1<BSendConfirm, String> {
        public Log__LinkName(BSendConfirm self, String value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._LinkName = this.getValue(); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Beans.Provider.BSendConfirm: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ConfirmSerialId").append('=').append(getConfirmSerialId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LinkName").append('=').append(getLinkName()).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int getPreAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void setPreAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
    }

    @SuppressWarnings("UnusedAssignment")
    @Override
    public void Encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            long _x_ = getConfirmSerialId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getLinkName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @SuppressWarnings("UnusedAssignment")
    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setConfirmSerialId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setLinkName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean NegativeCheck() {
        if (getConfirmSerialId() < 0)
            return true;
        return false;
    }
}
