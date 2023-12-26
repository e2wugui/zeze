// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BClearInUse extends Zeze.Transaction.Bean implements BClearInUseReadOnly {
    public static final long TYPEID = -5497586704851966855L;

    public static final int eSuccess = 0;
    public static final int eDefaultError = 1;
    public static final int eInstanceNotExists = 2;

    private int _LocalId; // serverId
    private String _Global; // global config, not use now, reserve for strict check.

    @Override
    public int getLocalId() {
        if (!isManaged())
            return _LocalId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _LocalId;
        var log = (Log__LocalId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _LocalId;
    }

    public void setLocalId(int value) {
        if (!isManaged()) {
            _LocalId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__LocalId(this, 1, value));
    }

    @Override
    public String getGlobal() {
        if (!isManaged())
            return _Global;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Global;
        var log = (Log__Global)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Global;
    }

    public void setGlobal(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Global = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Global(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BClearInUse() {
        _Global = "";
    }

    @SuppressWarnings("deprecation")
    public BClearInUse(int _LocalId_, String _Global_) {
        _LocalId = _LocalId_;
        if (_Global_ == null)
            _Global_ = "";
        _Global = _Global_;
    }

    @Override
    public void reset() {
        setLocalId(0);
        setGlobal("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BClearInUse.Data toData() {
        var data = new Zeze.Builtin.Dbh2.Master.BClearInUse.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.Master.BClearInUse.Data)other);
    }

    public void assign(BClearInUse.Data other) {
        setLocalId(other._LocalId);
        setGlobal(other._Global);
        _unknown_ = null;
    }

    public void assign(BClearInUse other) {
        setLocalId(other.getLocalId());
        setGlobal(other.getGlobal());
        _unknown_ = other._unknown_;
    }

    public BClearInUse copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BClearInUse copy() {
        var copy = new BClearInUse();
        copy.assign(this);
        return copy;
    }

    public static void swap(BClearInUse a, BClearInUse b) {
        BClearInUse save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__LocalId extends Zeze.Transaction.Logs.LogInt {
        public Log__LocalId(BClearInUse bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BClearInUse)getBelong())._LocalId = value; }
    }

    private static final class Log__Global extends Zeze.Transaction.Logs.LogString {
        public Log__Global(BClearInUse bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BClearInUse)getBelong())._Global = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Master.BClearInUse: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("LocalId=").append(getLocalId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Global=").append(getGlobal()).append(System.lineSeparator());
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
            int _x_ = getLocalId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = getGlobal();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
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
            setLocalId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setGlobal(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getLocalId() < 0)
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
                case 1: _LocalId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _Global = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setLocalId(rs.getInt(_parents_name_ + "LocalId"));
        setGlobal(rs.getString(_parents_name_ + "Global"));
        if (getGlobal() == null)
            setGlobal("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "LocalId", getLocalId());
        st.appendString(_parents_name_ + "Global", getGlobal());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "LocalId", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Global", "string", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -5497586704851966855L;

    public static final int eSuccess = 0;
    public static final int eDefaultError = 1;
    public static final int eInstanceNotExists = 2;

    private int _LocalId; // serverId
    private String _Global; // global config, not use now, reserve for strict check.

    public int getLocalId() {
        return _LocalId;
    }

    public void setLocalId(int value) {
        _LocalId = value;
    }

    public String getGlobal() {
        return _Global;
    }

    public void setGlobal(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Global = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Global = "";
    }

    @SuppressWarnings("deprecation")
    public Data(int _LocalId_, String _Global_) {
        _LocalId = _LocalId_;
        if (_Global_ == null)
            _Global_ = "";
        _Global = _Global_;
    }

    @Override
    public void reset() {
        _LocalId = 0;
        _Global = "";
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BClearInUse toBean() {
        var bean = new Zeze.Builtin.Dbh2.Master.BClearInUse();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BClearInUse)other);
    }

    public void assign(BClearInUse other) {
        _LocalId = other.getLocalId();
        _Global = other.getGlobal();
    }

    public void assign(BClearInUse.Data other) {
        _LocalId = other._LocalId;
        _Global = other._Global;
    }

    @Override
    public BClearInUse.Data copy() {
        var copy = new BClearInUse.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BClearInUse.Data a, BClearInUse.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BClearInUse.Data clone() {
        return (BClearInUse.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Master.BClearInUse: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("LocalId=").append(_LocalId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Global=").append(_Global).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

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
            int _x_ = _LocalId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = _Global;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _LocalId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Global = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
