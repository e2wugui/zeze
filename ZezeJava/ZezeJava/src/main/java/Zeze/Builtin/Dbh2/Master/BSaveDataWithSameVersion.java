// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BSaveDataWithSameVersion extends Zeze.Transaction.Bean implements BSaveDataWithSameVersionReadOnly {
    public static final long TYPEID = -2351623358007351459L;

    public static final int eSuccess = 0;
    public static final int eDefaultError = 1;
    public static final int eVersionMismatch = 2;
    public static final int eUpdateError = 3;

    private Zeze.Net.Binary _Key;
    private Zeze.Net.Binary _Data;
    private long _Version;

    @Override
    public Zeze.Net.Binary getKey() {
        if (!isManaged())
            return _Key;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Key;
        var log = (Log__Key)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Key;
    }

    public void setKey(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Key = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Key(this, 1, value));
    }

    @Override
    public Zeze.Net.Binary getData() {
        if (!isManaged())
            return _Data;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Data;
        var log = (Log__Data)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Data;
    }

    public void setData(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Data = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Data(this, 2, value));
    }

    @Override
    public long getVersion() {
        if (!isManaged())
            return _Version;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Version;
        var log = (Log__Version)txn.getLog(objectId() + 3);
        return log != null ? log.value : _Version;
    }

    public void setVersion(long value) {
        if (!isManaged()) {
            _Version = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Version(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BSaveDataWithSameVersion() {
        _Key = Zeze.Net.Binary.Empty;
        _Data = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BSaveDataWithSameVersion(Zeze.Net.Binary _Key_, Zeze.Net.Binary _Data_, long _Version_) {
        if (_Key_ == null)
            _Key_ = Zeze.Net.Binary.Empty;
        _Key = _Key_;
        if (_Data_ == null)
            _Data_ = Zeze.Net.Binary.Empty;
        _Data = _Data_;
        _Version = _Version_;
    }

    @Override
    public void reset() {
        setKey(Zeze.Net.Binary.Empty);
        setData(Zeze.Net.Binary.Empty);
        setVersion(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersion.Data toData() {
        var data = new Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersion.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersion.Data)other);
    }

    public void assign(BSaveDataWithSameVersion.Data other) {
        setKey(other._Key);
        setData(other._Data);
        setVersion(other._Version);
        _unknown_ = null;
    }

    public void assign(BSaveDataWithSameVersion other) {
        setKey(other.getKey());
        setData(other.getData());
        setVersion(other.getVersion());
        _unknown_ = other._unknown_;
    }

    public BSaveDataWithSameVersion copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSaveDataWithSameVersion copy() {
        var copy = new BSaveDataWithSameVersion();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSaveDataWithSameVersion a, BSaveDataWithSameVersion b) {
        BSaveDataWithSameVersion save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Key extends Zeze.Transaction.Logs.LogBinary {
        public Log__Key(BSaveDataWithSameVersion bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSaveDataWithSameVersion)getBelong())._Key = value; }
    }

    private static final class Log__Data extends Zeze.Transaction.Logs.LogBinary {
        public Log__Data(BSaveDataWithSameVersion bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSaveDataWithSameVersion)getBelong())._Data = value; }
    }

    private static final class Log__Version extends Zeze.Transaction.Logs.LogLong {
        public Log__Version(BSaveDataWithSameVersion bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSaveDataWithSameVersion)getBelong())._Version = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersion: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(getKey()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Data=").append(getData()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Version=").append(getVersion()).append(System.lineSeparator());
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
            var _x_ = getKey();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = getData();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            long _x_ = getVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
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
            setKey(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setData(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BSaveDataWithSameVersion))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSaveDataWithSameVersion)_o_;
        if (!getKey().equals(_b_.getKey()))
            return false;
        if (!getData().equals(_b_.getData()))
            return false;
        if (getVersion() != _b_.getVersion())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getVersion() < 0)
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
                case 1: _Key = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 2: _Data = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 3: _Version = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setKey(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "Key")));
        setData(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "Data")));
        setVersion(rs.getLong(_parents_name_ + "Version"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendBinary(_parents_name_ + "Key", getKey());
        st.appendBinary(_parents_name_ + "Data", getData());
        st.appendLong(_parents_name_ + "Version", getVersion());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Key", "binary", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Data", "binary", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Version", "long", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -2351623358007351459L;

    public static final int eSuccess = 0;
    public static final int eDefaultError = 1;
    public static final int eVersionMismatch = 2;
    public static final int eUpdateError = 3;

    private Zeze.Net.Binary _Key;
    private Zeze.Net.Binary _Data;
    private long _Version;

    public Zeze.Net.Binary getKey() {
        return _Key;
    }

    public void setKey(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Key = value;
    }

    public Zeze.Net.Binary getData() {
        return _Data;
    }

    public void setData(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Data = value;
    }

    public long getVersion() {
        return _Version;
    }

    public void setVersion(long value) {
        _Version = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Key = Zeze.Net.Binary.Empty;
        _Data = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Net.Binary _Key_, Zeze.Net.Binary _Data_, long _Version_) {
        if (_Key_ == null)
            _Key_ = Zeze.Net.Binary.Empty;
        _Key = _Key_;
        if (_Data_ == null)
            _Data_ = Zeze.Net.Binary.Empty;
        _Data = _Data_;
        _Version = _Version_;
    }

    @Override
    public void reset() {
        _Key = Zeze.Net.Binary.Empty;
        _Data = Zeze.Net.Binary.Empty;
        _Version = 0;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersion toBean() {
        var bean = new Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersion();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BSaveDataWithSameVersion)other);
    }

    public void assign(BSaveDataWithSameVersion other) {
        _Key = other.getKey();
        _Data = other.getData();
        _Version = other.getVersion();
    }

    public void assign(BSaveDataWithSameVersion.Data other) {
        _Key = other._Key;
        _Data = other._Data;
        _Version = other._Version;
    }

    @Override
    public BSaveDataWithSameVersion.Data copy() {
        var copy = new BSaveDataWithSameVersion.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSaveDataWithSameVersion.Data a, BSaveDataWithSameVersion.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BSaveDataWithSameVersion.Data clone() {
        return (BSaveDataWithSameVersion.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersion: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_Key).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Data=").append(_Data).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Version=").append(_Version).append(System.lineSeparator());
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
            var _x_ = _Key;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = _Data;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            long _x_ = _Version;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Key = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Data = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _Version = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
