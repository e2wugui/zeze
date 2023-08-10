// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

// 内置条件类型：到达位置
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BTConditionReachPosition extends Zeze.Transaction.Bean implements BTConditionReachPositionReadOnly {
    public static final long TYPEID = 3710715051036431511L;

    private int _dimension; // 2D | 3D
    private double _x;
    private double _y;
    private double _z;
    private double _radius;
    private boolean _Reached;

    @Override
    public int getDimension() {
        if (!isManaged())
            return _dimension;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _dimension;
        var log = (Log__dimension)txn.getLog(objectId() + 1);
        return log != null ? log.value : _dimension;
    }

    public void setDimension(int value) {
        if (!isManaged()) {
            _dimension = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__dimension(this, 1, value));
    }

    @Override
    public double getX() {
        if (!isManaged())
            return _x;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _x;
        var log = (Log__x)txn.getLog(objectId() + 2);
        return log != null ? log.value : _x;
    }

    public void setX(double value) {
        if (!isManaged()) {
            _x = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__x(this, 2, value));
    }

    @Override
    public double getY() {
        if (!isManaged())
            return _y;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _y;
        var log = (Log__y)txn.getLog(objectId() + 3);
        return log != null ? log.value : _y;
    }

    public void setY(double value) {
        if (!isManaged()) {
            _y = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__y(this, 3, value));
    }

    @Override
    public double getZ() {
        if (!isManaged())
            return _z;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _z;
        var log = (Log__z)txn.getLog(objectId() + 4);
        return log != null ? log.value : _z;
    }

    public void setZ(double value) {
        if (!isManaged()) {
            _z = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__z(this, 4, value));
    }

    @Override
    public double getRadius() {
        if (!isManaged())
            return _radius;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _radius;
        var log = (Log__radius)txn.getLog(objectId() + 5);
        return log != null ? log.value : _radius;
    }

    public void setRadius(double value) {
        if (!isManaged()) {
            _radius = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__radius(this, 5, value));
    }

    @Override
    public boolean isReached() {
        if (!isManaged())
            return _Reached;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Reached;
        var log = (Log__Reached)txn.getLog(objectId() + 6);
        return log != null ? log.value : _Reached;
    }

    public void setReached(boolean value) {
        if (!isManaged()) {
            _Reached = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Reached(this, 6, value));
    }

    @SuppressWarnings("deprecation")
    public BTConditionReachPosition() {
    }

    @SuppressWarnings("deprecation")
    public BTConditionReachPosition(int _dimension_, double _x_, double _y_, double _z_, double _radius_, boolean _Reached_) {
        _dimension = _dimension_;
        _x = _x_;
        _y = _y_;
        _z = _z_;
        _radius = _radius_;
        _Reached = _Reached_;
    }

    @Override
    public void reset() {
        setDimension(0);
        setX(0);
        setY(0);
        setZ(0);
        setRadius(0);
        setReached(false);
        _unknown_ = null;
    }

    public void assign(BTConditionReachPosition other) {
        setDimension(other.getDimension());
        setX(other.getX());
        setY(other.getY());
        setZ(other.getZ());
        setRadius(other.getRadius());
        setReached(other.isReached());
        _unknown_ = other._unknown_;
    }

    public BTConditionReachPosition copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTConditionReachPosition copy() {
        var copy = new BTConditionReachPosition();
        copy.assign(this);
        return copy;
    }

    public static void swap(BTConditionReachPosition a, BTConditionReachPosition b) {
        BTConditionReachPosition save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__dimension extends Zeze.Transaction.Logs.LogInt {
        public Log__dimension(BTConditionReachPosition bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTConditionReachPosition)getBelong())._dimension = value; }
    }

    private static final class Log__x extends Zeze.Transaction.Logs.LogDouble {
        public Log__x(BTConditionReachPosition bean, int varId, double value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTConditionReachPosition)getBelong())._x = value; }
    }

    private static final class Log__y extends Zeze.Transaction.Logs.LogDouble {
        public Log__y(BTConditionReachPosition bean, int varId, double value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTConditionReachPosition)getBelong())._y = value; }
    }

    private static final class Log__z extends Zeze.Transaction.Logs.LogDouble {
        public Log__z(BTConditionReachPosition bean, int varId, double value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTConditionReachPosition)getBelong())._z = value; }
    }

    private static final class Log__radius extends Zeze.Transaction.Logs.LogDouble {
        public Log__radius(BTConditionReachPosition bean, int varId, double value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTConditionReachPosition)getBelong())._radius = value; }
    }

    private static final class Log__Reached extends Zeze.Transaction.Logs.LogBool {
        public Log__Reached(BTConditionReachPosition bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTConditionReachPosition)getBelong())._Reached = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskBase.BTConditionReachPosition: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("dimension=").append(getDimension()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("x=").append(getX()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("y=").append(getY()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("z=").append(getZ()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("radius=").append(getRadius()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Reached=").append(isReached()).append(System.lineSeparator());
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
            int _x_ = getDimension();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            double _x_ = getX();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.DOUBLE);
                _o_.WriteDouble(_x_);
            }
        }
        {
            double _x_ = getY();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.DOUBLE);
                _o_.WriteDouble(_x_);
            }
        }
        {
            double _x_ = getZ();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.DOUBLE);
                _o_.WriteDouble(_x_);
            }
        }
        {
            double _x_ = getRadius();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.DOUBLE);
                _o_.WriteDouble(_x_);
            }
        }
        {
            boolean _x_ = isReached();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
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
            setDimension(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setX(_o_.ReadDouble(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setY(_o_.ReadDouble(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setZ(_o_.ReadDouble(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setRadius(_o_.ReadDouble(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setReached(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getDimension() < 0)
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
                case 1: _dimension = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _x = ((Zeze.Transaction.Logs.LogDouble)vlog).value; break;
                case 3: _y = ((Zeze.Transaction.Logs.LogDouble)vlog).value; break;
                case 4: _z = ((Zeze.Transaction.Logs.LogDouble)vlog).value; break;
                case 5: _radius = ((Zeze.Transaction.Logs.LogDouble)vlog).value; break;
                case 6: _Reached = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setDimension(rs.getInt(_parents_name_ + "dimension"));
        setX(rs.getDouble(_parents_name_ + "x"));
        setY(rs.getDouble(_parents_name_ + "y"));
        setZ(rs.getDouble(_parents_name_ + "z"));
        setRadius(rs.getDouble(_parents_name_ + "radius"));
        setReached(rs.getBoolean(_parents_name_ + "Reached"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "dimension", getDimension());
        st.appendDouble(_parents_name_ + "x", getX());
        st.appendDouble(_parents_name_ + "y", getY());
        st.appendDouble(_parents_name_ + "z", getZ());
        st.appendDouble(_parents_name_ + "radius", getRadius());
        st.appendBoolean(_parents_name_ + "Reached", isReached());
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BTConditionReachPosition
    }
}
