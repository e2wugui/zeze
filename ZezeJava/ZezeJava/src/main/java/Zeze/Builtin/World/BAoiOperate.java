// auto-generated @formatter:off
package Zeze.Builtin.World;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BAoiOperate extends Zeze.Transaction.Bean implements BAoiOperateReadOnly {
    public static final long TYPEID = 7467019147847621003L;

    private Zeze.Builtin.World.BObjectId _ObjectId;
    private int _OperateId;
    private Zeze.Net.Binary _Param;

    @Override
    public Zeze.Builtin.World.BObjectId getObjectId() {
        if (!isManaged())
            return _ObjectId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ObjectId;
        var log = (Log__ObjectId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _ObjectId;
    }

    public void setObjectId(Zeze.Builtin.World.BObjectId value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ObjectId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ObjectId(this, 1, value));
    }

    @Override
    public int getOperateId() {
        if (!isManaged())
            return _OperateId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _OperateId;
        var log = (Log__OperateId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _OperateId;
    }

    public void setOperateId(int value) {
        if (!isManaged()) {
            _OperateId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__OperateId(this, 2, value));
    }

    @Override
    public Zeze.Net.Binary getParam() {
        if (!isManaged())
            return _Param;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Param;
        var log = (Log__Param)txn.getLog(objectId() + 3);
        return log != null ? log.value : _Param;
    }

    public void setParam(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Param = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Param(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BAoiOperate() {
        _ObjectId = new Zeze.Builtin.World.BObjectId();
        _Param = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BAoiOperate(Zeze.Builtin.World.BObjectId _ObjectId_, int _OperateId_, Zeze.Net.Binary _Param_) {
        if (_ObjectId_ == null)
            _ObjectId_ = new Zeze.Builtin.World.BObjectId();
        _ObjectId = _ObjectId_;
        _OperateId = _OperateId_;
        if (_Param_ == null)
            _Param_ = Zeze.Net.Binary.Empty;
        _Param = _Param_;
    }

    @Override
    public void reset() {
        setObjectId(new Zeze.Builtin.World.BObjectId());
        setOperateId(0);
        setParam(Zeze.Net.Binary.Empty);
    }

    @Override
    public Zeze.Builtin.World.BAoiOperate.Data toData() {
        var data = new Zeze.Builtin.World.BAoiOperate.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.World.BAoiOperate.Data)other);
    }

    public void assign(BAoiOperate.Data other) {
        setObjectId(other._ObjectId);
        setOperateId(other._OperateId);
        setParam(other._Param);
    }

    public void assign(BAoiOperate other) {
        setObjectId(other.getObjectId());
        setOperateId(other.getOperateId());
        setParam(other.getParam());
    }

    public BAoiOperate copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAoiOperate copy() {
        var copy = new BAoiOperate();
        copy.assign(this);
        return copy;
    }

    public static void swap(BAoiOperate a, BAoiOperate b) {
        BAoiOperate save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ObjectId extends Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.World.BObjectId> {
        public Log__ObjectId(BAoiOperate bean, int varId, Zeze.Builtin.World.BObjectId value) { super(Zeze.Builtin.World.BObjectId.class, bean, varId, value); }

        @Override
        public void commit() { ((BAoiOperate)getBelong())._ObjectId = value; }
    }

    private static final class Log__OperateId extends Zeze.Transaction.Logs.LogInt {
        public Log__OperateId(BAoiOperate bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAoiOperate)getBelong())._OperateId = value; }
    }

    private static final class Log__Param extends Zeze.Transaction.Logs.LogBinary {
        public Log__Param(BAoiOperate bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAoiOperate)getBelong())._Param = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BAoiOperate: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ObjectId=").append(System.lineSeparator());
        getObjectId().buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("OperateId=").append(getOperateId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Param=").append(getParam()).append(System.lineSeparator());
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
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getObjectId().encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            int _x_ = getOperateId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = getParam();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _o_.ReadBean(getObjectId(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setOperateId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setParam(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean negativeCheck() {
        if (getObjectId().negativeCheck())
            return true;
        if (getOperateId() < 0)
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
                case 1: _ObjectId = ((Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.World.BObjectId>)vlog).value; break;
                case 2: _OperateId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _Param = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        parents.add("ObjectId");
        getObjectId().decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setOperateId(rs.getInt(_parents_name_ + "OperateId"));
        setParam(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "Param")));
        if (getParam() == null)
            setParam(Zeze.Net.Binary.Empty);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        parents.add("ObjectId");
        getObjectId().encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "OperateId", getOperateId());
        st.appendBinary(_parents_name_ + "Param", getParam());
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 7467019147847621003L;

    private Zeze.Builtin.World.BObjectId _ObjectId;
    private int _OperateId;
    private Zeze.Net.Binary _Param;

    public Zeze.Builtin.World.BObjectId getObjectId() {
        return _ObjectId;
    }

    public void setObjectId(Zeze.Builtin.World.BObjectId value) {
        if (value == null)
            throw new IllegalArgumentException();
        _ObjectId = value;
    }

    public int getOperateId() {
        return _OperateId;
    }

    public void setOperateId(int value) {
        _OperateId = value;
    }

    public Zeze.Net.Binary getParam() {
        return _Param;
    }

    public void setParam(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Param = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _ObjectId = new Zeze.Builtin.World.BObjectId();
        _Param = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Builtin.World.BObjectId _ObjectId_, int _OperateId_, Zeze.Net.Binary _Param_) {
        if (_ObjectId_ == null)
            _ObjectId_ = new Zeze.Builtin.World.BObjectId();
        _ObjectId = _ObjectId_;
        _OperateId = _OperateId_;
        if (_Param_ == null)
            _Param_ = Zeze.Net.Binary.Empty;
        _Param = _Param_;
    }

    @Override
    public void reset() {
        _ObjectId = new Zeze.Builtin.World.BObjectId();
        _OperateId = 0;
        _Param = Zeze.Net.Binary.Empty;
    }

    @Override
    public Zeze.Builtin.World.BAoiOperate toBean() {
        var bean = new Zeze.Builtin.World.BAoiOperate();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BAoiOperate)other);
    }

    public void assign(BAoiOperate other) {
        _ObjectId = other.getObjectId();
        _OperateId = other.getOperateId();
        _Param = other.getParam();
    }

    public void assign(BAoiOperate.Data other) {
        _ObjectId = other._ObjectId;
        _OperateId = other._OperateId;
        _Param = other._Param;
    }

    @Override
    public BAoiOperate.Data copy() {
        var copy = new BAoiOperate.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BAoiOperate.Data a, BAoiOperate.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BAoiOperate.Data clone() {
        return (BAoiOperate.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BAoiOperate: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ObjectId=").append(System.lineSeparator());
        _ObjectId.buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("OperateId=").append(_OperateId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Param=").append(_Param).append(System.lineSeparator());
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
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _ObjectId.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            int _x_ = _OperateId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _Param;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _o_.ReadBean(_ObjectId, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _OperateId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _Param = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
