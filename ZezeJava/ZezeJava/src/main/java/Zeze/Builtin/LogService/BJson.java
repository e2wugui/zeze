// auto-generated @formatter:off
package Zeze.Builtin.LogService;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BJson extends Zeze.Transaction.Bean implements BJsonReadOnly {
    public static final long TYPEID = 2743837942654367657L;

    private String _Json;

    @Override
    public String getJson() {
        if (!isManaged())
            return _Json;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Json;
        var log = (Log__Json)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Json;
    }

    public void setJson(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Json = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Json(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BJson() {
        _Json = "";
    }

    @SuppressWarnings("deprecation")
    public BJson(String _Json_) {
        if (_Json_ == null)
            _Json_ = "";
        _Json = _Json_;
    }

    @Override
    public void reset() {
        setJson("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.LogService.BJson.Data toData() {
        var data = new Zeze.Builtin.LogService.BJson.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.LogService.BJson.Data)other);
    }

    public void assign(BJson.Data other) {
        setJson(other._Json);
        _unknown_ = null;
    }

    public void assign(BJson other) {
        setJson(other.getJson());
        _unknown_ = other._unknown_;
    }

    public BJson copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BJson copy() {
        var copy = new BJson();
        copy.assign(this);
        return copy;
    }

    public static void swap(BJson a, BJson b) {
        BJson save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Json extends Zeze.Transaction.Logs.LogString {
        public Log__Json(BJson bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BJson)getBelong())._Json = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.LogService.BJson: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Json=").append(getJson()).append(System.lineSeparator());
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
            String _x_ = getJson();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
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
            setJson(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BJson))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BJson)_o_;
        if (!getJson().equals(_b_.getJson()))
            return false;
        return true;
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
                case 1: _Json = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setJson(rs.getString(_parents_name_ + "Json"));
        if (getJson() == null)
            setJson("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Json", getJson());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Json", "string", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 2743837942654367657L;

    private String _Json;

    public String getJson() {
        return _Json;
    }

    public void setJson(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Json = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Json = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _Json_) {
        if (_Json_ == null)
            _Json_ = "";
        _Json = _Json_;
    }

    @Override
    public void reset() {
        _Json = "";
    }

    @Override
    public Zeze.Builtin.LogService.BJson toBean() {
        var bean = new Zeze.Builtin.LogService.BJson();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BJson)other);
    }

    public void assign(BJson other) {
        _Json = other.getJson();
    }

    public void assign(BJson.Data other) {
        _Json = other._Json;
    }

    @Override
    public BJson.Data copy() {
        var copy = new BJson.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BJson.Data a, BJson.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BJson.Data clone() {
        return (BJson.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.LogService.BJson: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Json=").append(_Json).append(System.lineSeparator());
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
            String _x_ = _Json;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
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
            _Json = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
