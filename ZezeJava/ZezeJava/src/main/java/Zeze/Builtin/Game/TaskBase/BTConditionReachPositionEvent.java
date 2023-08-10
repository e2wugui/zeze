// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BTConditionReachPositionEvent extends Zeze.Transaction.Bean implements BTConditionReachPositionEventReadOnly {
    public static final long TYPEID = -7787690739413726977L;

    private double _x;
    private double _y;
    private double _z;

    @Override
    public double getX() {
        if (!isManaged())
            return _x;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _x;
        var log = (Log__x)txn.getLog(objectId() + 1);
        return log != null ? log.value : _x;
    }

    public void setX(double value) {
        if (!isManaged()) {
            _x = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__x(this, 1, value));
    }

    @Override
    public double getY() {
        if (!isManaged())
            return _y;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _y;
        var log = (Log__y)txn.getLog(objectId() + 2);
        return log != null ? log.value : _y;
    }

    public void setY(double value) {
        if (!isManaged()) {
            _y = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__y(this, 2, value));
    }

    @Override
    public double getZ() {
        if (!isManaged())
            return _z;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _z;
        var log = (Log__z)txn.getLog(objectId() + 3);
        return log != null ? log.value : _z;
    }

    public void setZ(double value) {
        if (!isManaged()) {
            _z = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__z(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BTConditionReachPositionEvent() {
    }

    @SuppressWarnings("deprecation")
    public BTConditionReachPositionEvent(double _x_, double _y_, double _z_) {
        _x = _x_;
        _y = _y_;
        _z = _z_;
    }

    @Override
    public void reset() {
        setX(0);
        setY(0);
        setZ(0);
        _unknown_ = null;
    }

    public void assign(BTConditionReachPositionEvent other) {
        setX(other.getX());
        setY(other.getY());
        setZ(other.getZ());
        _unknown_ = other._unknown_;
    }

    public BTConditionReachPositionEvent copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTConditionReachPositionEvent copy() {
        var copy = new BTConditionReachPositionEvent();
        copy.assign(this);
        return copy;
    }

    public static void swap(BTConditionReachPositionEvent a, BTConditionReachPositionEvent b) {
        BTConditionReachPositionEvent save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__x extends Zeze.Transaction.Logs.LogDouble {
        public Log__x(BTConditionReachPositionEvent bean, int varId, double value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTConditionReachPositionEvent)getBelong())._x = value; }
    }

    private static final class Log__y extends Zeze.Transaction.Logs.LogDouble {
        public Log__y(BTConditionReachPositionEvent bean, int varId, double value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTConditionReachPositionEvent)getBelong())._y = value; }
    }

    private static final class Log__z extends Zeze.Transaction.Logs.LogDouble {
        public Log__z(BTConditionReachPositionEvent bean, int varId, double value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTConditionReachPositionEvent)getBelong())._z = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskBase.BTConditionReachPositionEvent: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("x=").append(getX()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("y=").append(getY()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("z=").append(getZ()).append(System.lineSeparator());
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
            double _x_ = getX();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.DOUBLE);
                _o_.WriteDouble(_x_);
            }
        }
        {
            double _x_ = getY();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.DOUBLE);
                _o_.WriteDouble(_x_);
            }
        }
        {
            double _x_ = getZ();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.DOUBLE);
                _o_.WriteDouble(_x_);
            }
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setX(_o_.ReadDouble(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setY(_o_.ReadDouble(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setZ(_o_.ReadDouble(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
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
                case 1: _x = ((Zeze.Transaction.Logs.LogDouble)vlog).value; break;
                case 2: _y = ((Zeze.Transaction.Logs.LogDouble)vlog).value; break;
                case 3: _z = ((Zeze.Transaction.Logs.LogDouble)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setX(rs.getDouble(_parents_name_ + "x"));
        setY(rs.getDouble(_parents_name_ + "y"));
        setZ(rs.getDouble(_parents_name_ + "z"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendDouble(_parents_name_ + "x", getX());
        st.appendDouble(_parents_name_ + "y", getY());
        st.appendDouble(_parents_name_ + "z", getZ());
    }

    @Override
    public java.util.List<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "x", "double", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "y", "double", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "z", "double", "", ""));
        return vars;
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BTConditionReachPositionEvent
    }
}
