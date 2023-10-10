// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BKick extends Zeze.Transaction.Bean implements BKickReadOnly {
    public static final long TYPEID = -6855697390328479333L;

    public static final int ErrorProtocolUnknown = 1;
    public static final int ErrorDecode = 2;
    public static final int ErrorProtocolException = 3;
    public static final int ErrorDuplicateLogin = 4;
    public static final int ErrorSeeDescription = 5;
    public static final int ErrorOnlineSetName = 6;
    public static final int ErrorStopServer = 7;
    public static final int eControlClose = 0; // 通过ReportError报告给客户端，并关闭链接。
    public static final int eControlReportClient = 1; // 通过ReportError报告给客户端，不关闭链接。
    public static final int eControlReportLinkd = 2; // Linkd收到自行做些处理。

    private long _linksid;
    private int _code;
    private String _desc; // for debug
    private int _control;

    @Override
    public long getLinksid() {
        if (!isManaged())
            return _linksid;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _linksid;
        var log = (Log__linksid)txn.getLog(objectId() + 1);
        return log != null ? log.value : _linksid;
    }

    public void setLinksid(long value) {
        if (!isManaged()) {
            _linksid = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__linksid(this, 1, value));
    }

    @Override
    public int getCode() {
        if (!isManaged())
            return _code;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _code;
        var log = (Log__code)txn.getLog(objectId() + 2);
        return log != null ? log.value : _code;
    }

    public void setCode(int value) {
        if (!isManaged()) {
            _code = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__code(this, 2, value));
    }

    @Override
    public String getDesc() {
        if (!isManaged())
            return _desc;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _desc;
        var log = (Log__desc)txn.getLog(objectId() + 3);
        return log != null ? log.value : _desc;
    }

    public void setDesc(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _desc = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__desc(this, 3, value));
    }

    @Override
    public int getControl() {
        if (!isManaged())
            return _control;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _control;
        var log = (Log__control)txn.getLog(objectId() + 4);
        return log != null ? log.value : _control;
    }

    public void setControl(int value) {
        if (!isManaged()) {
            _control = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__control(this, 4, value));
    }

    @SuppressWarnings("deprecation")
    public BKick() {
        _desc = "";
    }

    @SuppressWarnings("deprecation")
    public BKick(long _linksid_, int _code_, String _desc_, int _control_) {
        _linksid = _linksid_;
        _code = _code_;
        if (_desc_ == null)
            _desc_ = "";
        _desc = _desc_;
        _control = _control_;
    }

    @Override
    public void reset() {
        setLinksid(0);
        setCode(0);
        setDesc("");
        setControl(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Provider.BKick.Data toData() {
        var data = new Zeze.Builtin.Provider.BKick.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Provider.BKick.Data)other);
    }

    public void assign(BKick.Data other) {
        setLinksid(other._linksid);
        setCode(other._code);
        setDesc(other._desc);
        setControl(other._control);
        _unknown_ = null;
    }

    public void assign(BKick other) {
        setLinksid(other.getLinksid());
        setCode(other.getCode());
        setDesc(other.getDesc());
        setControl(other.getControl());
        _unknown_ = other._unknown_;
    }

    public BKick copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BKick copy() {
        var copy = new BKick();
        copy.assign(this);
        return copy;
    }

    public static void swap(BKick a, BKick b) {
        BKick save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__linksid extends Zeze.Transaction.Logs.LogLong {
        public Log__linksid(BKick bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BKick)getBelong())._linksid = value; }
    }

    private static final class Log__code extends Zeze.Transaction.Logs.LogInt {
        public Log__code(BKick bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BKick)getBelong())._code = value; }
    }

    private static final class Log__desc extends Zeze.Transaction.Logs.LogString {
        public Log__desc(BKick bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BKick)getBelong())._desc = value; }
    }

    private static final class Log__control extends Zeze.Transaction.Logs.LogInt {
        public Log__control(BKick bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BKick)getBelong())._control = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BKick: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("linksid=").append(getLinksid()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("code=").append(getCode()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("desc=").append(getDesc()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("control=").append(getControl()).append(System.lineSeparator());
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
            long _x_ = getLinksid();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getCode();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = getDesc();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getControl();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
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
            setLinksid(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setCode(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setDesc(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setControl(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getLinksid() < 0)
            return true;
        if (getCode() < 0)
            return true;
        if (getControl() < 0)
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
                case 1: _linksid = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _code = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _desc = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 4: _control = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setLinksid(rs.getLong(_parents_name_ + "linksid"));
        setCode(rs.getInt(_parents_name_ + "code"));
        setDesc(rs.getString(_parents_name_ + "desc"));
        if (getDesc() == null)
            setDesc("");
        setControl(rs.getInt(_parents_name_ + "control"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "linksid", getLinksid());
        st.appendInt(_parents_name_ + "code", getCode());
        st.appendString(_parents_name_ + "desc", getDesc());
        st.appendInt(_parents_name_ + "control", getControl());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "linksid", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "code", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "desc", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "control", "int", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -6855697390328479333L;

    public static final int ErrorProtocolUnknown = 1;
    public static final int ErrorDecode = 2;
    public static final int ErrorProtocolException = 3;
    public static final int ErrorDuplicateLogin = 4;
    public static final int ErrorSeeDescription = 5;
    public static final int ErrorOnlineSetName = 6;
    public static final int ErrorStopServer = 7;
    public static final int eControlClose = 0; // 通过ReportError报告给客户端，并关闭链接。
    public static final int eControlReportClient = 1; // 通过ReportError报告给客户端，不关闭链接。
    public static final int eControlReportLinkd = 2; // Linkd收到自行做些处理。

    private long _linksid;
    private int _code;
    private String _desc; // for debug
    private int _control;

    public long getLinksid() {
        return _linksid;
    }

    public void setLinksid(long value) {
        _linksid = value;
    }

    public int getCode() {
        return _code;
    }

    public void setCode(int value) {
        _code = value;
    }

    public String getDesc() {
        return _desc;
    }

    public void setDesc(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _desc = value;
    }

    public int getControl() {
        return _control;
    }

    public void setControl(int value) {
        _control = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _desc = "";
    }

    @SuppressWarnings("deprecation")
    public Data(long _linksid_, int _code_, String _desc_, int _control_) {
        _linksid = _linksid_;
        _code = _code_;
        if (_desc_ == null)
            _desc_ = "";
        _desc = _desc_;
        _control = _control_;
    }

    @Override
    public void reset() {
        _linksid = 0;
        _code = 0;
        _desc = "";
        _control = 0;
    }

    @Override
    public Zeze.Builtin.Provider.BKick toBean() {
        var bean = new Zeze.Builtin.Provider.BKick();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BKick)other);
    }

    public void assign(BKick other) {
        _linksid = other.getLinksid();
        _code = other.getCode();
        _desc = other.getDesc();
        _control = other.getControl();
    }

    public void assign(BKick.Data other) {
        _linksid = other._linksid;
        _code = other._code;
        _desc = other._desc;
        _control = other._control;
    }

    @Override
    public BKick.Data copy() {
        var copy = new BKick.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BKick.Data a, BKick.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BKick.Data clone() {
        return (BKick.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BKick: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("linksid=").append(_linksid).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("code=").append(_code).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("desc=").append(_desc).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("control=").append(_control).append(System.lineSeparator());
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
            long _x_ = _linksid;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = _code;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = _desc;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = _control;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
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
            _linksid = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _code = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _desc = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _control = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
