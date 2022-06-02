// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BDestroy extends Zeze.Transaction.Bean {
    private String _BagName;
    private int _Position;

    public String getBagName() {
        if (!isManaged())
            return _BagName;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _BagName;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__BagName)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.Value : _BagName;
    }

    public void setBagName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _BagName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__BagName(this, 1, value));
    }

    public int getPosition() {
        if (!isManaged())
            return _Position;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _Position;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Position)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.Value : _Position;
    }

    public void setPosition(int value) {
        if (!isManaged()) {
            _Position = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Position(this, 2, value));
    }

    public BDestroy() {
         this(0);
    }

    public BDestroy(int _varId_) {
        super(_varId_);
        _BagName = "";
    }

    public void Assign(BDestroy other) {
        setBagName(other.getBagName());
        setPosition(other.getPosition());
    }

    public BDestroy CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BDestroy Copy() {
        var copy = new BDestroy();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BDestroy a, BDestroy b) {
        BDestroy save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -3139270057603893776L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__BagName extends Zeze.Transaction.Logs.LogString {
        public Log__BagName(BDestroy bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BDestroy)getBelong())._BagName = Value; }
    }

    private static final class Log__Position extends Zeze.Transaction.Logs.LogInt {
        public Log__Position(BDestroy bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BDestroy)getBelong())._Position = Value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Bag.BDestroy: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("BagName").append('=').append(getBagName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Position").append('=').append(getPosition()).append(System.lineSeparator());
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

    @Override
    public void Encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            String _x_ = getBagName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getPosition();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setBagName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setPosition(_o_.ReadInt(_t_));
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

    @Override
    public boolean NegativeCheck() {
        if (getPosition() < 0)
            return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void FollowerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _BagName = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
                case 2: _Position = ((Zeze.Transaction.Logs.LogInt)vlog).Value; break;
            }
        }
    }
}
