// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BMove extends Zeze.Transaction.Bean {
    private String _BagName;
    private int _PositionFrom;
    private int _PositionTo;
    private int _number; // -1 表示全部

    public String getBagName() {
        if (!isManaged())
            return _BagName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _BagName;
        var log = (Log__BagName)txn.GetLog(objectId() + 1);
        return log != null ? log.Value : _BagName;
    }

    public void setBagName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _BagName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__BagName(this, 1, value));
    }

    public int getPositionFrom() {
        if (!isManaged())
            return _PositionFrom;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _PositionFrom;
        var log = (Log__PositionFrom)txn.GetLog(objectId() + 2);
        return log != null ? log.Value : _PositionFrom;
    }

    public void setPositionFrom(int value) {
        if (!isManaged()) {
            _PositionFrom = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__PositionFrom(this, 2, value));
    }

    public int getPositionTo() {
        if (!isManaged())
            return _PositionTo;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _PositionTo;
        var log = (Log__PositionTo)txn.GetLog(objectId() + 3);
        return log != null ? log.Value : _PositionTo;
    }

    public void setPositionTo(int value) {
        if (!isManaged()) {
            _PositionTo = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__PositionTo(this, 3, value));
    }

    public int getNumber() {
        if (!isManaged())
            return _number;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _number;
        var log = (Log__number)txn.GetLog(objectId() + 4);
        return log != null ? log.Value : _number;
    }

    public void setNumber(int value) {
        if (!isManaged()) {
            _number = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__number(this, 4, value));
    }

    public BMove() {
        _BagName = "";
    }

    public BMove(String _BagName_, int _PositionFrom_, int _PositionTo_, int _number_) {
        if (_BagName_ == null)
            throw new IllegalArgumentException();
        _BagName = _BagName_;
        _PositionFrom = _PositionFrom_;
        _PositionTo = _PositionTo_;
        _number = _number_;
    }

    public void Assign(BMove other) {
        setBagName(other.getBagName());
        setPositionFrom(other.getPositionFrom());
        setPositionTo(other.getPositionTo());
        setNumber(other.getNumber());
    }

    public BMove CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BMove Copy() {
        var copy = new BMove();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BMove a, BMove b) {
        BMove save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public BMove CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -7346236832819011963L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__BagName extends Zeze.Transaction.Logs.LogString {
        public Log__BagName(BMove bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BMove)getBelong())._BagName = Value; }
    }

    private static final class Log__PositionFrom extends Zeze.Transaction.Logs.LogInt {
        public Log__PositionFrom(BMove bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BMove)getBelong())._PositionFrom = Value; }
    }

    private static final class Log__PositionTo extends Zeze.Transaction.Logs.LogInt {
        public Log__PositionTo(BMove bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BMove)getBelong())._PositionTo = Value; }
    }

    private static final class Log__number extends Zeze.Transaction.Logs.LogInt {
        public Log__number(BMove bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BMove)getBelong())._number = Value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void BuildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Bag.BMove: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("BagName").append('=').append(getBagName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PositionFrom").append('=').append(getPositionFrom()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PositionTo").append('=').append(getPositionTo()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("number").append('=').append(getNumber()).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int size) {
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
            int _x_ = getPositionFrom();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getPositionTo();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getNumber();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
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
            setPositionFrom(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setPositionTo(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setNumber(_o_.ReadInt(_t_));
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
    protected void ResetChildrenRootInfo() {
    }

    @Override
    public boolean NegativeCheck() {
        if (getPositionFrom() < 0)
            return true;
        if (getPositionTo() < 0)
            return true;
        if (getNumber() < 0)
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
                case 2: _PositionFrom = ((Zeze.Transaction.Logs.LogInt)vlog).Value; break;
                case 3: _PositionTo = ((Zeze.Transaction.Logs.LogInt)vlog).Value; break;
                case 4: _number = ((Zeze.Transaction.Logs.LogInt)vlog).Value; break;
            }
        }
    }
}
