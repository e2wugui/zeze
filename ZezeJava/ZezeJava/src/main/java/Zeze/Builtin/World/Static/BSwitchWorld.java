// auto-generated @formatter:off
package Zeze.Builtin.World.Static;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BSwitchWorld extends Zeze.Transaction.Bean implements BSwitchWorldReadOnly {
    public static final long TYPEID = -2702601729537956678L;

    private int _MapId;
    private int _FromMapId;
    private int _FromGateId;

    @Override
    public int getMapId() {
        if (!isManaged())
            return _MapId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _MapId;
        var log = (Log__MapId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _MapId;
    }

    public void setMapId(int value) {
        if (!isManaged()) {
            _MapId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__MapId(this, 1, value));
    }

    @Override
    public int getFromMapId() {
        if (!isManaged())
            return _FromMapId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _FromMapId;
        var log = (Log__FromMapId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _FromMapId;
    }

    public void setFromMapId(int value) {
        if (!isManaged()) {
            _FromMapId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__FromMapId(this, 2, value));
    }

    @Override
    public int getFromGateId() {
        if (!isManaged())
            return _FromGateId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _FromGateId;
        var log = (Log__FromGateId)txn.getLog(objectId() + 3);
        return log != null ? log.value : _FromGateId;
    }

    public void setFromGateId(int value) {
        if (!isManaged()) {
            _FromGateId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__FromGateId(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BSwitchWorld() {
    }

    @SuppressWarnings("deprecation")
    public BSwitchWorld(int _MapId_, int _FromMapId_, int _FromGateId_) {
        _MapId = _MapId_;
        _FromMapId = _FromMapId_;
        _FromGateId = _FromGateId_;
    }

    @Override
    public void reset() {
        setMapId(0);
        setFromMapId(0);
        setFromGateId(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.World.Static.BSwitchWorld.Data toData() {
        var data = new Zeze.Builtin.World.Static.BSwitchWorld.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.World.Static.BSwitchWorld.Data)other);
    }

    public void assign(BSwitchWorld.Data other) {
        setMapId(other._MapId);
        setFromMapId(other._FromMapId);
        setFromGateId(other._FromGateId);
        _unknown_ = null;
    }

    public void assign(BSwitchWorld other) {
        setMapId(other.getMapId());
        setFromMapId(other.getFromMapId());
        setFromGateId(other.getFromGateId());
        _unknown_ = other._unknown_;
    }

    public BSwitchWorld copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSwitchWorld copy() {
        var copy = new BSwitchWorld();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSwitchWorld a, BSwitchWorld b) {
        BSwitchWorld save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__MapId extends Zeze.Transaction.Logs.LogInt {
        public Log__MapId(BSwitchWorld bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSwitchWorld)getBelong())._MapId = value; }
    }

    private static final class Log__FromMapId extends Zeze.Transaction.Logs.LogInt {
        public Log__FromMapId(BSwitchWorld bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSwitchWorld)getBelong())._FromMapId = value; }
    }

    private static final class Log__FromGateId extends Zeze.Transaction.Logs.LogInt {
        public Log__FromGateId(BSwitchWorld bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSwitchWorld)getBelong())._FromGateId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.Static.BSwitchWorld: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("MapId=").append(getMapId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("FromMapId=").append(getFromMapId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("FromGateId=").append(getFromGateId()).append(System.lineSeparator());
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
            int _x_ = getMapId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getFromMapId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getFromGateId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
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
            setMapId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setFromMapId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setFromGateId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getMapId() < 0)
            return true;
        if (getFromMapId() < 0)
            return true;
        if (getFromGateId() < 0)
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
                case 1: _MapId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _FromMapId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _FromGateId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setMapId(rs.getInt(_parents_name_ + "MapId"));
        setFromMapId(rs.getInt(_parents_name_ + "FromMapId"));
        setFromGateId(rs.getInt(_parents_name_ + "FromGateId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "MapId", getMapId());
        st.appendInt(_parents_name_ + "FromMapId", getFromMapId());
        st.appendInt(_parents_name_ + "FromGateId", getFromGateId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "MapId", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "FromMapId", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "FromGateId", "int", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -2702601729537956678L;

    private int _MapId;
    private int _FromMapId;
    private int _FromGateId;

    public int getMapId() {
        return _MapId;
    }

    public void setMapId(int value) {
        _MapId = value;
    }

    public int getFromMapId() {
        return _FromMapId;
    }

    public void setFromMapId(int value) {
        _FromMapId = value;
    }

    public int getFromGateId() {
        return _FromGateId;
    }

    public void setFromGateId(int value) {
        _FromGateId = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(int _MapId_, int _FromMapId_, int _FromGateId_) {
        _MapId = _MapId_;
        _FromMapId = _FromMapId_;
        _FromGateId = _FromGateId_;
    }

    @Override
    public void reset() {
        _MapId = 0;
        _FromMapId = 0;
        _FromGateId = 0;
    }

    @Override
    public Zeze.Builtin.World.Static.BSwitchWorld toBean() {
        var bean = new Zeze.Builtin.World.Static.BSwitchWorld();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BSwitchWorld)other);
    }

    public void assign(BSwitchWorld other) {
        _MapId = other.getMapId();
        _FromMapId = other.getFromMapId();
        _FromGateId = other.getFromGateId();
    }

    public void assign(BSwitchWorld.Data other) {
        _MapId = other._MapId;
        _FromMapId = other._FromMapId;
        _FromGateId = other._FromGateId;
    }

    @Override
    public BSwitchWorld.Data copy() {
        var copy = new BSwitchWorld.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSwitchWorld.Data a, BSwitchWorld.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BSwitchWorld.Data clone() {
        return (BSwitchWorld.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.Static.BSwitchWorld: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("MapId=").append(_MapId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("FromMapId=").append(_FromMapId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("FromGateId=").append(_FromGateId).append(System.lineSeparator());
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
            int _x_ = _MapId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _FromMapId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _FromGateId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _MapId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _FromMapId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _FromGateId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
