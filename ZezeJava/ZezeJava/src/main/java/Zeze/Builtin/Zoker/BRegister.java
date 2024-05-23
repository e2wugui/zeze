// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BRegister extends Zeze.Transaction.Bean implements BRegisterReadOnly {
    public static final long TYPEID = -6705392585502947760L;

    private String _ZokerName;

    @Override
    public String getZokerName() {
        if (!isManaged())
            return _ZokerName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ZokerName;
        var log = (Log__ZokerName)txn.getLog(objectId() + 1);
        return log != null ? log.value : _ZokerName;
    }

    public void setZokerName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ZokerName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ZokerName(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BRegister() {
        _ZokerName = "";
    }

    @SuppressWarnings("deprecation")
    public BRegister(String _ZokerName_) {
        if (_ZokerName_ == null)
            _ZokerName_ = "";
        _ZokerName = _ZokerName_;
    }

    @Override
    public void reset() {
        setZokerName("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Zoker.BRegister.Data toData() {
        var data = new Zeze.Builtin.Zoker.BRegister.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Zoker.BRegister.Data)other);
    }

    public void assign(BRegister.Data other) {
        setZokerName(other._ZokerName);
        _unknown_ = null;
    }

    public void assign(BRegister other) {
        setZokerName(other.getZokerName());
        _unknown_ = other._unknown_;
    }

    public BRegister copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BRegister copy() {
        var copy = new BRegister();
        copy.assign(this);
        return copy;
    }

    public static void swap(BRegister a, BRegister b) {
        BRegister save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ZokerName extends Zeze.Transaction.Logs.LogString {
        public Log__ZokerName(BRegister bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BRegister)getBelong())._ZokerName = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Zoker.BRegister: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ZokerName=").append(getZokerName()).append(System.lineSeparator());
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
            String _x_ = getZokerName();
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
            setZokerName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BRegister))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BRegister)_o_;
        if (!getZokerName().equals(_b_.getZokerName()))
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
                case 1: _ZokerName = vlog.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setZokerName(rs.getString(_parents_name_ + "ZokerName"));
        if (getZokerName() == null)
            setZokerName("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "ZokerName", getZokerName());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ZokerName", "string", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -6705392585502947760L;

    private String _ZokerName;

    public String getZokerName() {
        return _ZokerName;
    }

    public void setZokerName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _ZokerName = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _ZokerName = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _ZokerName_) {
        if (_ZokerName_ == null)
            _ZokerName_ = "";
        _ZokerName = _ZokerName_;
    }

    @Override
    public void reset() {
        _ZokerName = "";
    }

    @Override
    public Zeze.Builtin.Zoker.BRegister toBean() {
        var bean = new Zeze.Builtin.Zoker.BRegister();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BRegister)other);
    }

    public void assign(BRegister other) {
        _ZokerName = other.getZokerName();
    }

    public void assign(BRegister.Data other) {
        _ZokerName = other._ZokerName;
    }

    @Override
    public BRegister.Data copy() {
        var copy = new BRegister.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BRegister.Data a, BRegister.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BRegister.Data clone() {
        return (BRegister.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Zoker.BRegister: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ZokerName=").append(_ZokerName).append(System.lineSeparator());
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
            String _x_ = _ZokerName;
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
            _ZokerName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
