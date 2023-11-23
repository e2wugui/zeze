// auto-generated @formatter:off
package Zeze.Builtin.HotDistribute;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BVariable extends Zeze.Transaction.Bean implements BVariableReadOnly {
    public static final long TYPEID = 7877437207710416076L;

    private int _Id;
    private String _Name;
    private String _Type;
    private String _Key;
    private String _Value;

    @Override
    public int getId() {
        if (!isManaged())
            return _Id;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Id;
        var log = (Log__Id)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Id;
    }

    public void setId(int value) {
        if (!isManaged()) {
            _Id = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Id(this, 1, value));
    }

    @Override
    public String getName() {
        if (!isManaged())
            return _Name;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Name;
        var log = (Log__Name)txn.getLog(objectId() + 2);
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
        txn.putLog(new Log__Name(this, 2, value));
    }

    @Override
    public String getType() {
        if (!isManaged())
            return _Type;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Type;
        var log = (Log__Type)txn.getLog(objectId() + 3);
        return log != null ? log.value : _Type;
    }

    public void setType(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Type = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Type(this, 3, value));
    }

    @Override
    public String getKey() {
        if (!isManaged())
            return _Key;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Key;
        var log = (Log__Key)txn.getLog(objectId() + 4);
        return log != null ? log.value : _Key;
    }

    public void setKey(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Key = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Key(this, 4, value));
    }

    @Override
    public String getValue() {
        if (!isManaged())
            return _Value;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Value;
        var log = (Log__Value)txn.getLog(objectId() + 5);
        return log != null ? log.value : _Value;
    }

    public void setValue(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Value = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Value(this, 5, value));
    }

    @SuppressWarnings("deprecation")
    public BVariable() {
        _Name = "";
        _Type = "";
        _Key = "";
        _Value = "";
    }

    @SuppressWarnings("deprecation")
    public BVariable(int _Id_, String _Name_, String _Type_, String _Key_, String _Value_) {
        _Id = _Id_;
        if (_Name_ == null)
            _Name_ = "";
        _Name = _Name_;
        if (_Type_ == null)
            _Type_ = "";
        _Type = _Type_;
        if (_Key_ == null)
            _Key_ = "";
        _Key = _Key_;
        if (_Value_ == null)
            _Value_ = "";
        _Value = _Value_;
    }

    @Override
    public void reset() {
        setId(0);
        setName("");
        setType("");
        setKey("");
        setValue("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.HotDistribute.BVariable.Data toData() {
        var data = new Zeze.Builtin.HotDistribute.BVariable.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.HotDistribute.BVariable.Data)other);
    }

    public void assign(BVariable.Data other) {
        setId(other._Id);
        setName(other._Name);
        setType(other._Type);
        setKey(other._Key);
        setValue(other._Value);
        _unknown_ = null;
    }

    public void assign(BVariable other) {
        setId(other.getId());
        setName(other.getName());
        setType(other.getType());
        setKey(other.getKey());
        setValue(other.getValue());
        _unknown_ = other._unknown_;
    }

    public BVariable copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BVariable copy() {
        var copy = new BVariable();
        copy.assign(this);
        return copy;
    }

    public static void swap(BVariable a, BVariable b) {
        BVariable save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Id extends Zeze.Transaction.Logs.LogInt {
        public Log__Id(BVariable bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BVariable)getBelong())._Id = value; }
    }

    private static final class Log__Name extends Zeze.Transaction.Logs.LogString {
        public Log__Name(BVariable bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BVariable)getBelong())._Name = value; }
    }

    private static final class Log__Type extends Zeze.Transaction.Logs.LogString {
        public Log__Type(BVariable bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BVariable)getBelong())._Type = value; }
    }

    private static final class Log__Key extends Zeze.Transaction.Logs.LogString {
        public Log__Key(BVariable bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BVariable)getBelong())._Key = value; }
    }

    private static final class Log__Value extends Zeze.Transaction.Logs.LogString {
        public Log__Value(BVariable bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BVariable)getBelong())._Value = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.HotDistribute.BVariable: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Id=").append(getId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Name=").append(getName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Type=").append(getType()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(getKey()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(getValue()).append(System.lineSeparator());
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
            int _x_ = getId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = getName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getType();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getKey();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getValue();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
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
            setId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setType(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setKey(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setValue(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getId() < 0)
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
                case 1: _Id = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _Name = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _Type = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 4: _Key = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 5: _Value = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setId(rs.getInt(_parents_name_ + "Id"));
        setName(rs.getString(_parents_name_ + "Name"));
        if (getName() == null)
            setName("");
        setType(rs.getString(_parents_name_ + "Type"));
        if (getType() == null)
            setType("");
        setKey(rs.getString(_parents_name_ + "Key"));
        if (getKey() == null)
            setKey("");
        setValue(rs.getString(_parents_name_ + "Value"));
        if (getValue() == null)
            setValue("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "Id", getId());
        st.appendString(_parents_name_ + "Name", getName());
        st.appendString(_parents_name_ + "Type", getType());
        st.appendString(_parents_name_ + "Key", getKey());
        st.appendString(_parents_name_ + "Value", getValue());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Id", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Name", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Type", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Key", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "Value", "string", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 7877437207710416076L;

    private int _Id;
    private String _Name;
    private String _Type;
    private String _Key;
    private String _Value;

    public int getId() {
        return _Id;
    }

    public void setId(int value) {
        _Id = value;
    }

    public String getName() {
        return _Name;
    }

    public void setName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Name = value;
    }

    public String getType() {
        return _Type;
    }

    public void setType(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Type = value;
    }

    public String getKey() {
        return _Key;
    }

    public void setKey(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Key = value;
    }

    public String getValue() {
        return _Value;
    }

    public void setValue(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Value = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Name = "";
        _Type = "";
        _Key = "";
        _Value = "";
    }

    @SuppressWarnings("deprecation")
    public Data(int _Id_, String _Name_, String _Type_, String _Key_, String _Value_) {
        _Id = _Id_;
        if (_Name_ == null)
            _Name_ = "";
        _Name = _Name_;
        if (_Type_ == null)
            _Type_ = "";
        _Type = _Type_;
        if (_Key_ == null)
            _Key_ = "";
        _Key = _Key_;
        if (_Value_ == null)
            _Value_ = "";
        _Value = _Value_;
    }

    @Override
    public void reset() {
        _Id = 0;
        _Name = "";
        _Type = "";
        _Key = "";
        _Value = "";
    }

    @Override
    public Zeze.Builtin.HotDistribute.BVariable toBean() {
        var bean = new Zeze.Builtin.HotDistribute.BVariable();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BVariable)other);
    }

    public void assign(BVariable other) {
        _Id = other.getId();
        _Name = other.getName();
        _Type = other.getType();
        _Key = other.getKey();
        _Value = other.getValue();
    }

    public void assign(BVariable.Data other) {
        _Id = other._Id;
        _Name = other._Name;
        _Type = other._Type;
        _Key = other._Key;
        _Value = other._Value;
    }

    @Override
    public BVariable.Data copy() {
        var copy = new BVariable.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BVariable.Data a, BVariable.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BVariable.Data clone() {
        return (BVariable.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.HotDistribute.BVariable: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Id=").append(_Id).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Name=").append(_Name).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Type=").append(_Type).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_Key).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(_Value).append(System.lineSeparator());
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
            int _x_ = _Id;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = _Name;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = _Type;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = _Key;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = _Value;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
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
            _Id = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Name = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _Type = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _Key = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _Value = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
