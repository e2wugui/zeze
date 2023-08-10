// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BWalk extends Zeze.Transaction.Bean implements BWalkReadOnly {
    public static final long TYPEID = 2689376469133093665L;

    private Zeze.Net.Binary _ExclusiveStartKey;
    private int _ProposeLimit;
    private boolean _Desc;

    @Override
    public Zeze.Net.Binary getExclusiveStartKey() {
        if (!isManaged())
            return _ExclusiveStartKey;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ExclusiveStartKey;
        var log = (Log__ExclusiveStartKey)txn.getLog(objectId() + 1);
        return log != null ? log.value : _ExclusiveStartKey;
    }

    public void setExclusiveStartKey(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ExclusiveStartKey = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ExclusiveStartKey(this, 1, value));
    }

    @Override
    public int getProposeLimit() {
        if (!isManaged())
            return _ProposeLimit;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ProposeLimit;
        var log = (Log__ProposeLimit)txn.getLog(objectId() + 2);
        return log != null ? log.value : _ProposeLimit;
    }

    public void setProposeLimit(int value) {
        if (!isManaged()) {
            _ProposeLimit = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ProposeLimit(this, 2, value));
    }

    @Override
    public boolean isDesc() {
        if (!isManaged())
            return _Desc;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Desc;
        var log = (Log__Desc)txn.getLog(objectId() + 3);
        return log != null ? log.value : _Desc;
    }

    public void setDesc(boolean value) {
        if (!isManaged()) {
            _Desc = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Desc(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BWalk() {
        _ExclusiveStartKey = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BWalk(Zeze.Net.Binary _ExclusiveStartKey_, int _ProposeLimit_, boolean _Desc_) {
        if (_ExclusiveStartKey_ == null)
            _ExclusiveStartKey_ = Zeze.Net.Binary.Empty;
        _ExclusiveStartKey = _ExclusiveStartKey_;
        _ProposeLimit = _ProposeLimit_;
        _Desc = _Desc_;
    }

    @Override
    public void reset() {
        setExclusiveStartKey(Zeze.Net.Binary.Empty);
        setProposeLimit(0);
        setDesc(false);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.BWalk.Data toData() {
        var data = new Zeze.Builtin.Dbh2.BWalk.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.BWalk.Data)other);
    }

    public void assign(BWalk.Data other) {
        setExclusiveStartKey(other._ExclusiveStartKey);
        setProposeLimit(other._ProposeLimit);
        setDesc(other._Desc);
        _unknown_ = null;
    }

    public void assign(BWalk other) {
        setExclusiveStartKey(other.getExclusiveStartKey());
        setProposeLimit(other.getProposeLimit());
        setDesc(other.isDesc());
        _unknown_ = other._unknown_;
    }

    public BWalk copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BWalk copy() {
        var copy = new BWalk();
        copy.assign(this);
        return copy;
    }

    public static void swap(BWalk a, BWalk b) {
        BWalk save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ExclusiveStartKey extends Zeze.Transaction.Logs.LogBinary {
        public Log__ExclusiveStartKey(BWalk bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BWalk)getBelong())._ExclusiveStartKey = value; }
    }

    private static final class Log__ProposeLimit extends Zeze.Transaction.Logs.LogInt {
        public Log__ProposeLimit(BWalk bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BWalk)getBelong())._ProposeLimit = value; }
    }

    private static final class Log__Desc extends Zeze.Transaction.Logs.LogBool {
        public Log__Desc(BWalk bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BWalk)getBelong())._Desc = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BWalk: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ExclusiveStartKey=").append(getExclusiveStartKey()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProposeLimit=").append(getProposeLimit()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Desc=").append(isDesc()).append(System.lineSeparator());
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
            var _x_ = getExclusiveStartKey();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            int _x_ = getProposeLimit();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            boolean _x_ = isDesc();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
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
            setExclusiveStartKey(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setProposeLimit(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setDesc(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getProposeLimit() < 0)
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
                case 1: _ExclusiveStartKey = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 2: _ProposeLimit = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _Desc = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setExclusiveStartKey(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "ExclusiveStartKey")));
        if (getExclusiveStartKey() == null)
            setExclusiveStartKey(Zeze.Net.Binary.Empty);
        setProposeLimit(rs.getInt(_parents_name_ + "ProposeLimit"));
        setDesc(rs.getBoolean(_parents_name_ + "Desc"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendBinary(_parents_name_ + "ExclusiveStartKey", getExclusiveStartKey());
        st.appendInt(_parents_name_ + "ProposeLimit", getProposeLimit());
        st.appendBoolean(_parents_name_ + "Desc", isDesc());
    }

    @Override
    public java.util.List<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ExclusiveStartKey", "binary", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ProposeLimit", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Desc", "bool", "", ""));
        return vars;
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BWalk
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 2689376469133093665L;

    private Zeze.Net.Binary _ExclusiveStartKey;
    private int _ProposeLimit;
    private boolean _Desc;

    public Zeze.Net.Binary getExclusiveStartKey() {
        return _ExclusiveStartKey;
    }

    public void setExclusiveStartKey(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _ExclusiveStartKey = value;
    }

    public int getProposeLimit() {
        return _ProposeLimit;
    }

    public void setProposeLimit(int value) {
        _ProposeLimit = value;
    }

    public boolean isDesc() {
        return _Desc;
    }

    public void setDesc(boolean value) {
        _Desc = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _ExclusiveStartKey = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Net.Binary _ExclusiveStartKey_, int _ProposeLimit_, boolean _Desc_) {
        if (_ExclusiveStartKey_ == null)
            _ExclusiveStartKey_ = Zeze.Net.Binary.Empty;
        _ExclusiveStartKey = _ExclusiveStartKey_;
        _ProposeLimit = _ProposeLimit_;
        _Desc = _Desc_;
    }

    @Override
    public void reset() {
        _ExclusiveStartKey = Zeze.Net.Binary.Empty;
        _ProposeLimit = 0;
        _Desc = false;
    }

    @Override
    public Zeze.Builtin.Dbh2.BWalk toBean() {
        var bean = new Zeze.Builtin.Dbh2.BWalk();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BWalk)other);
    }

    public void assign(BWalk other) {
        _ExclusiveStartKey = other.getExclusiveStartKey();
        _ProposeLimit = other.getProposeLimit();
        _Desc = other.isDesc();
    }

    public void assign(BWalk.Data other) {
        _ExclusiveStartKey = other._ExclusiveStartKey;
        _ProposeLimit = other._ProposeLimit;
        _Desc = other._Desc;
    }

    @Override
    public BWalk.Data copy() {
        var copy = new BWalk.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BWalk.Data a, BWalk.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BWalk.Data clone() {
        return (BWalk.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BWalk: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ExclusiveStartKey=").append(_ExclusiveStartKey).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProposeLimit=").append(_ProposeLimit).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Desc=").append(_Desc).append(System.lineSeparator());
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
            var _x_ = _ExclusiveStartKey;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            int _x_ = _ProposeLimit;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            boolean _x_ = _Desc;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _ExclusiveStartKey = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _ProposeLimit = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _Desc = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
