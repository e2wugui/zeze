// auto-generated @formatter:off
package Zeze.Builtin.HotDistribute;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BeanName extends Zeze.Transaction.Bean implements BeanNameReadOnly {
    public static final long TYPEID = -1975096028535811269L;

    private String _Name;

    @Override
    public String getName() {
        if (!isManaged())
            return _Name;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Name;
        var log = (Log__Name)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Name;
    }

    public void setName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Name = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Name(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BeanName() {
        _Name = "";
    }

    @SuppressWarnings("deprecation")
    public BeanName(String _Name_) {
        if (_Name_ == null)
            _Name_ = "";
        _Name = _Name_;
    }

    @Override
    public void reset() {
        setName("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.HotDistribute.BeanName.Data toData() {
        var data = new Zeze.Builtin.HotDistribute.BeanName.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.HotDistribute.BeanName.Data)other);
    }

    public void assign(BeanName.Data other) {
        setName(other._Name);
        _unknown_ = null;
    }

    public void assign(BeanName other) {
        setName(other.getName());
        _unknown_ = other._unknown_;
    }

    public BeanName copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BeanName copy() {
        var copy = new BeanName();
        copy.assign(this);
        return copy;
    }

    public static void swap(BeanName a, BeanName b) {
        BeanName save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Name extends Zeze.Transaction.Logs.LogString {
        public Log__Name(BeanName bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BeanName)getBelong())._Name = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.HotDistribute.BeanName: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Name=").append(getName()).append(System.lineSeparator());
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
            String _x_ = getName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
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
            setName(_o_.ReadString(_t_));
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
                case 1: _Name = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setName(rs.getString(_parents_name_ + "Name"));
        if (getName() == null)
            setName("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Name", getName());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Name", "string", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -1975096028535811269L;

    private String _Name;

    public String getName() {
        return _Name;
    }

    public void setName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Name = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Name = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _Name_) {
        if (_Name_ == null)
            _Name_ = "";
        _Name = _Name_;
    }

    @Override
    public void reset() {
        _Name = "";
    }

    @Override
    public Zeze.Builtin.HotDistribute.BeanName toBean() {
        var bean = new Zeze.Builtin.HotDistribute.BeanName();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BeanName)other);
    }

    public void assign(BeanName other) {
        _Name = other.getName();
    }

    public void assign(BeanName.Data other) {
        _Name = other._Name;
    }

    @Override
    public BeanName.Data copy() {
        var copy = new BeanName.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BeanName.Data a, BeanName.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BeanName.Data clone() {
        return (BeanName.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.HotDistribute.BeanName: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Name=").append(_Name).append(System.lineSeparator());
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
            String _x_ = _Name;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Name = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
