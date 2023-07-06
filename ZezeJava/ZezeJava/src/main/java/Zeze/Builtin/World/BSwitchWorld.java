// auto-generated @formatter:off
package Zeze.Builtin.World;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BSwitchWorld extends Zeze.Transaction.Bean implements BSwitchWorldReadOnly {
    public static final long TYPEID = 463822314787890974L;

    private int _MapId;

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

    @SuppressWarnings("deprecation")
    public BSwitchWorld() {
    }

    @SuppressWarnings("deprecation")
    public BSwitchWorld(int _MapId_) {
        _MapId = _MapId_;
    }

    @Override
    public void reset() {
        setMapId(0);
    }

    @Override
    public Zeze.Builtin.World.BSwitchWorld.Data toData() {
        var data = new Zeze.Builtin.World.BSwitchWorld.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.World.BSwitchWorld.Data)other);
    }

    public void assign(BSwitchWorld.Data other) {
        setMapId(other._MapId);
    }

    public void assign(BSwitchWorld other) {
        setMapId(other.getMapId());
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

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BSwitchWorld: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("MapId=").append(getMapId()).append(System.lineSeparator());
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
            int _x_ = getMapId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
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
            setMapId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean negativeCheck() {
        if (getMapId() < 0)
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
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setMapId(rs.getInt(_parents_name_ + "MapId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "MapId", getMapId());
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 463822314787890974L;

    private int _MapId;

    public int getMapId() {
        return _MapId;
    }

    public void setMapId(int value) {
        _MapId = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(int _MapId_) {
        _MapId = _MapId_;
    }

    @Override
    public void reset() {
        _MapId = 0;
    }

    @Override
    public Zeze.Builtin.World.BSwitchWorld toBean() {
        var bean = new Zeze.Builtin.World.BSwitchWorld();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BSwitchWorld)other);
    }

    public void assign(BSwitchWorld other) {
        _MapId = other.getMapId();
    }

    public void assign(BSwitchWorld.Data other) {
        _MapId = other._MapId;
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BSwitchWorld: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("MapId=").append(_MapId).append(System.lineSeparator());
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
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
