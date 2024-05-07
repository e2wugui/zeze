// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BMove extends Zeze.Transaction.Bean implements BMoveReadOnly {
    public static final long TYPEID = -7346236832819011963L;

    private String _BagName;
    private int _PositionFrom;
    private int _PositionTo;
    private int _number; // -1 表示全部

    @Override
    public String getBagName() {
        if (!isManaged())
            return _BagName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _BagName;
        var log = (Log__BagName)txn.getLog(objectId() + 1);
        return log != null ? log.value : _BagName;
    }

    public void setBagName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _BagName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__BagName(this, 1, value));
    }

    @Override
    public int getPositionFrom() {
        if (!isManaged())
            return _PositionFrom;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _PositionFrom;
        var log = (Log__PositionFrom)txn.getLog(objectId() + 2);
        return log != null ? log.value : _PositionFrom;
    }

    public void setPositionFrom(int value) {
        if (!isManaged()) {
            _PositionFrom = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__PositionFrom(this, 2, value));
    }

    @Override
    public int getPositionTo() {
        if (!isManaged())
            return _PositionTo;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _PositionTo;
        var log = (Log__PositionTo)txn.getLog(objectId() + 3);
        return log != null ? log.value : _PositionTo;
    }

    public void setPositionTo(int value) {
        if (!isManaged()) {
            _PositionTo = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__PositionTo(this, 3, value));
    }

    @Override
    public int getNumber() {
        if (!isManaged())
            return _number;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _number;
        var log = (Log__number)txn.getLog(objectId() + 4);
        return log != null ? log.value : _number;
    }

    public void setNumber(int value) {
        if (!isManaged()) {
            _number = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__number(this, 4, value));
    }

    @SuppressWarnings("deprecation")
    public BMove() {
        _BagName = "";
    }

    @SuppressWarnings("deprecation")
    public BMove(String _BagName_, int _PositionFrom_, int _PositionTo_, int _number_) {
        if (_BagName_ == null)
            _BagName_ = "";
        _BagName = _BagName_;
        _PositionFrom = _PositionFrom_;
        _PositionTo = _PositionTo_;
        _number = _number_;
    }

    @Override
    public void reset() {
        setBagName("");
        setPositionFrom(0);
        setPositionTo(0);
        setNumber(0);
        _unknown_ = null;
    }

    public void assign(BMove other) {
        setBagName(other.getBagName());
        setPositionFrom(other.getPositionFrom());
        setPositionTo(other.getPositionTo());
        setNumber(other.getNumber());
        _unknown_ = other._unknown_;
    }

    public BMove copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BMove copy() {
        var copy = new BMove();
        copy.assign(this);
        return copy;
    }

    public static void swap(BMove a, BMove b) {
        BMove save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__BagName extends Zeze.Transaction.Logs.LogString {
        public Log__BagName(BMove bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BMove)getBelong())._BagName = value; }
    }

    private static final class Log__PositionFrom extends Zeze.Transaction.Logs.LogInt {
        public Log__PositionFrom(BMove bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BMove)getBelong())._PositionFrom = value; }
    }

    private static final class Log__PositionTo extends Zeze.Transaction.Logs.LogInt {
        public Log__PositionTo(BMove bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BMove)getBelong())._PositionTo = value; }
    }

    private static final class Log__number extends Zeze.Transaction.Logs.LogInt {
        public Log__number(BMove bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BMove)getBelong())._number = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Bag.BMove: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("BagName=").append(getBagName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PositionFrom=").append(getPositionFrom()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PositionTo=").append(getPositionTo()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("number=").append(getNumber()).append(System.lineSeparator());
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

    private byte[] _unknown_;

    public byte[] unknown() {
        return _unknown_;
    }

    public void clearUnknown() {
        _unknown_ = null;
    }

    @Override
    public void encode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        var _ua_ = _unknown_;
        var _ui_ = _ua_ != null ? (_u_ = ByteBuffer.Wrap(_ua_)).readUnknownIndex() : Long.MAX_VALUE;
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
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        ByteBuffer _u_ = null;
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
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BMove))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BMove)_o_;
        if (!getBagName().equals(_b_.getBagName()))
            return false;
        if (getPositionFrom() != _b_.getPositionFrom())
            return false;
        if (getPositionTo() != _b_.getPositionTo())
            return false;
        if (getNumber() != _b_.getNumber())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
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
    public void followerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _BagName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _PositionFrom = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _PositionTo = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 4: _number = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setBagName(rs.getString(_parents_name_ + "BagName"));
        if (getBagName() == null)
            setBagName("");
        setPositionFrom(rs.getInt(_parents_name_ + "PositionFrom"));
        setPositionTo(rs.getInt(_parents_name_ + "PositionTo"));
        setNumber(rs.getInt(_parents_name_ + "number"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "BagName", getBagName());
        st.appendInt(_parents_name_ + "PositionFrom", getPositionFrom());
        st.appendInt(_parents_name_ + "PositionTo", getPositionTo());
        st.appendInt(_parents_name_ + "number", getNumber());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "BagName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "PositionFrom", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "PositionTo", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "number", "int", "", ""));
        return vars;
    }
}
