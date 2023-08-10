// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BUserState extends Zeze.Transaction.Bean implements BUserStateReadOnly {
    public static final long TYPEID = 5802054934505091577L;

    private String _context;
    private Zeze.Net.Binary _contextx;
    private String _onlineSetName;

    @Override
    public String getContext() {
        if (!isManaged())
            return _context;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _context;
        var log = (Log__context)txn.getLog(objectId() + 1);
        return log != null ? log.value : _context;
    }

    public void setContext(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _context = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__context(this, 1, value));
    }

    @Override
    public Zeze.Net.Binary getContextx() {
        if (!isManaged())
            return _contextx;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _contextx;
        var log = (Log__contextx)txn.getLog(objectId() + 2);
        return log != null ? log.value : _contextx;
    }

    public void setContextx(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _contextx = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__contextx(this, 2, value));
    }

    @Override
    public String getOnlineSetName() {
        if (!isManaged())
            return _onlineSetName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _onlineSetName;
        var log = (Log__onlineSetName)txn.getLog(objectId() + 3);
        return log != null ? log.value : _onlineSetName;
    }

    public void setOnlineSetName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _onlineSetName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__onlineSetName(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BUserState() {
        _context = "";
        _contextx = Zeze.Net.Binary.Empty;
        _onlineSetName = "";
    }

    @SuppressWarnings("deprecation")
    public BUserState(String _context_, Zeze.Net.Binary _contextx_, String _onlineSetName_) {
        if (_context_ == null)
            _context_ = "";
        _context = _context_;
        if (_contextx_ == null)
            _contextx_ = Zeze.Net.Binary.Empty;
        _contextx = _contextx_;
        if (_onlineSetName_ == null)
            _onlineSetName_ = "";
        _onlineSetName = _onlineSetName_;
    }

    @Override
    public void reset() {
        setContext("");
        setContextx(Zeze.Net.Binary.Empty);
        setOnlineSetName("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Provider.BUserState.Data toData() {
        var data = new Zeze.Builtin.Provider.BUserState.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Provider.BUserState.Data)other);
    }

    public void assign(BUserState.Data other) {
        setContext(other._context);
        setContextx(other._contextx);
        setOnlineSetName(other._onlineSetName);
        _unknown_ = null;
    }

    public void assign(BUserState other) {
        setContext(other.getContext());
        setContextx(other.getContextx());
        setOnlineSetName(other.getOnlineSetName());
        _unknown_ = other._unknown_;
    }

    public BUserState copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BUserState copy() {
        var copy = new BUserState();
        copy.assign(this);
        return copy;
    }

    public static void swap(BUserState a, BUserState b) {
        BUserState save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__context extends Zeze.Transaction.Logs.LogString {
        public Log__context(BUserState bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BUserState)getBelong())._context = value; }
    }

    private static final class Log__contextx extends Zeze.Transaction.Logs.LogBinary {
        public Log__contextx(BUserState bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BUserState)getBelong())._contextx = value; }
    }

    private static final class Log__onlineSetName extends Zeze.Transaction.Logs.LogString {
        public Log__onlineSetName(BUserState bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BUserState)getBelong())._onlineSetName = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BUserState: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("context=").append(getContext()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("contextx=").append(getContextx()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("onlineSetName=").append(getOnlineSetName()).append(System.lineSeparator());
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
            String _x_ = getContext();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getContextx();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            String _x_ = getOnlineSetName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
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
            setContext(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setContextx(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setOnlineSetName(_o_.ReadString(_t_));
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
                case 1: _context = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _contextx = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 3: _onlineSetName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setContext(rs.getString(_parents_name_ + "context"));
        if (getContext() == null)
            setContext("");
        setContextx(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "contextx")));
        if (getContextx() == null)
            setContextx(Zeze.Net.Binary.Empty);
        setOnlineSetName(rs.getString(_parents_name_ + "onlineSetName"));
        if (getOnlineSetName() == null)
            setOnlineSetName("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "context", getContext());
        st.appendBinary(_parents_name_ + "contextx", getContextx());
        st.appendString(_parents_name_ + "onlineSetName", getOnlineSetName());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "context", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "contextx", "binary", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "onlineSetName", "string", "", ""));
        return vars;
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BUserState
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 5802054934505091577L;

    private String _context;
    private Zeze.Net.Binary _contextx;
    private String _onlineSetName;

    public String getContext() {
        return _context;
    }

    public void setContext(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _context = value;
    }

    public Zeze.Net.Binary getContextx() {
        return _contextx;
    }

    public void setContextx(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _contextx = value;
    }

    public String getOnlineSetName() {
        return _onlineSetName;
    }

    public void setOnlineSetName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _onlineSetName = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _context = "";
        _contextx = Zeze.Net.Binary.Empty;
        _onlineSetName = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _context_, Zeze.Net.Binary _contextx_, String _onlineSetName_) {
        if (_context_ == null)
            _context_ = "";
        _context = _context_;
        if (_contextx_ == null)
            _contextx_ = Zeze.Net.Binary.Empty;
        _contextx = _contextx_;
        if (_onlineSetName_ == null)
            _onlineSetName_ = "";
        _onlineSetName = _onlineSetName_;
    }

    @Override
    public void reset() {
        _context = "";
        _contextx = Zeze.Net.Binary.Empty;
        _onlineSetName = "";
    }

    @Override
    public Zeze.Builtin.Provider.BUserState toBean() {
        var bean = new Zeze.Builtin.Provider.BUserState();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BUserState)other);
    }

    public void assign(BUserState other) {
        _context = other.getContext();
        _contextx = other.getContextx();
        _onlineSetName = other.getOnlineSetName();
    }

    public void assign(BUserState.Data other) {
        _context = other._context;
        _contextx = other._contextx;
        _onlineSetName = other._onlineSetName;
    }

    @Override
    public BUserState.Data copy() {
        var copy = new BUserState.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BUserState.Data a, BUserState.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BUserState.Data clone() {
        return (BUserState.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BUserState: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("context=").append(_context).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("contextx=").append(_contextx).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("onlineSetName=").append(_onlineSetName).append(System.lineSeparator());
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
            String _x_ = _context;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _contextx;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            String _x_ = _onlineSetName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
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
            _context = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _contextx = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _onlineSetName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
