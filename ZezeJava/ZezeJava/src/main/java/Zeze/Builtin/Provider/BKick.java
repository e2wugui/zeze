// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BKick extends Zeze.Transaction.Bean implements BKickReadOnly {
    public static final long TYPEID = -6855697390328479333L;

    public static final int ErrorProtocolUnknown = 1;
    public static final int ErrorDecode = 2;
    public static final int ErrorProtocolException = 3;
    public static final int ErrorDuplicateLogin = 4;
    public static final int ErrorSeeDescription = 5;
    public static final int ErrorOnlineSetName = 6;
    public static final int ErrorStopServer = 7;
    public static final int ErrorAuth = 8;
    public static final int eControlClose = 0; // 通过ReportError报告给客户端，并关闭链接。
    public static final int eControlReportClient = 1; // 通过ReportError报告给客户端，不关闭链接。
    public static final int eControlReportLinkd = 2; // Linkd收到自行做些处理。

    private long _linksid;
    private int _code;
    private String _desc; // for debug
    private int _control;

    private static final java.lang.invoke.VarHandle vh_linksid;
    private static final java.lang.invoke.VarHandle vh_code;
    private static final java.lang.invoke.VarHandle vh_desc;
    private static final java.lang.invoke.VarHandle vh_control;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_linksid = _l_.findVarHandle(BKick.class, "_linksid", long.class);
            vh_code = _l_.findVarHandle(BKick.class, "_code", int.class);
            vh_desc = _l_.findVarHandle(BKick.class, "_desc", String.class);
            vh_control = _l_.findVarHandle(BKick.class, "_control", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getLinksid() {
        if (!isManaged())
            return _linksid;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _linksid;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _linksid;
    }

    public void setLinksid(long _v_) {
        if (!isManaged()) {
            _linksid = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_linksid, _v_));
    }

    @Override
    public int getCode() {
        if (!isManaged())
            return _code;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _code;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _code;
    }

    public void setCode(int _v_) {
        if (!isManaged()) {
            _code = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 2, vh_code, _v_));
    }

    @Override
    public String getDesc() {
        if (!isManaged())
            return _desc;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _desc;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _desc;
    }

    public void setDesc(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _desc = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 3, vh_desc, _v_));
    }

    @Override
    public int getControl() {
        if (!isManaged())
            return _control;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _control;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _control;
    }

    public void setControl(int _v_) {
        if (!isManaged()) {
            _control = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 4, vh_control, _v_));
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
        var _d_ = new Zeze.Builtin.Provider.BKick.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Provider.BKick.Data)_o_);
    }

    public void assign(BKick.Data _o_) {
        setLinksid(_o_._linksid);
        setCode(_o_._code);
        setDesc(_o_._desc);
        setControl(_o_._control);
        _unknown_ = null;
    }

    public void assign(BKick _o_) {
        setLinksid(_o_.getLinksid());
        setCode(_o_.getCode());
        setDesc(_o_.getDesc());
        setControl(_o_.getControl());
        _unknown_ = _o_._unknown_;
    }

    public BKick copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BKick copy() {
        var _c_ = new BKick();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BKick _a_, BKick _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        _s_.append("Zeze.Builtin.Provider.BKick: {\n");
        _s_.append(_i1_).append("linksid=").append(getLinksid()).append(",\n");
        _s_.append(_i1_).append("code=").append(getCode()).append(",\n");
        _s_.append(_i1_).append("desc=").append(getDesc()).append(",\n");
        _s_.append(_i1_).append("control=").append(getControl()).append('\n');
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
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
    public void decode(IByteBuffer _o_) {
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
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BKick))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BKick)_o_;
        if (getLinksid() != _b_.getLinksid())
            return false;
        if (getCode() != _b_.getCode())
            return false;
        if (!getDesc().equals(_b_.getDesc()))
            return false;
        if (getControl() != _b_.getControl())
            return false;
        return true;
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
    public void followerApply(Zeze.Transaction.Log _l_) {
        var _vs_ = ((Zeze.Transaction.Collections.LogBean)_l_).getVariables();
        if (_vs_ == null)
            return;
        for (var _i_ = _vs_.iterator(); _i_.moveToNext(); ) {
            var _v_ = _i_.value();
            switch (_v_.getVariableId()) {
                case 1: _linksid = _v_.longValue(); break;
                case 2: _code = _v_.intValue(); break;
                case 3: _desc = _v_.stringValue(); break;
                case 4: _control = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setLinksid(_r_.getLong(_pn_ + "linksid"));
        setCode(_r_.getInt(_pn_ + "code"));
        setDesc(_r_.getString(_pn_ + "desc"));
        if (getDesc() == null)
            setDesc("");
        setControl(_r_.getInt(_pn_ + "control"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "linksid", getLinksid());
        _s_.appendInt(_pn_ + "code", getCode());
        _s_.appendString(_pn_ + "desc", getDesc());
        _s_.appendInt(_pn_ + "control", getControl());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "linksid", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "code", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "desc", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "control", "int", "", ""));
        return _v_;
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
    public static final int ErrorAuth = 8;
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

    public void setLinksid(long _v_) {
        _linksid = _v_;
    }

    public int getCode() {
        return _code;
    }

    public void setCode(int _v_) {
        _code = _v_;
    }

    public String getDesc() {
        return _desc;
    }

    public void setDesc(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _desc = _v_;
    }

    public int getControl() {
        return _control;
    }

    public void setControl(int _v_) {
        _control = _v_;
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
        var _b_ = new Zeze.Builtin.Provider.BKick();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BKick)_o_);
    }

    public void assign(BKick _o_) {
        _linksid = _o_.getLinksid();
        _code = _o_.getCode();
        _desc = _o_.getDesc();
        _control = _o_.getControl();
    }

    public void assign(BKick.Data _o_) {
        _linksid = _o_._linksid;
        _code = _o_._code;
        _desc = _o_._desc;
        _control = _o_._control;
    }

    @Override
    public BKick.Data copy() {
        var _c_ = new BKick.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BKick.Data _a_, BKick.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
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
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        _s_.append("Zeze.Builtin.Provider.BKick: {\n");
        _s_.append(_i1_).append("linksid=").append(_linksid).append(",\n");
        _s_.append(_i1_).append("code=").append(_code).append(",\n");
        _s_.append(_i1_).append("desc=").append(_desc).append(",\n");
        _s_.append(_i1_).append("control=").append(_control).append('\n');
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
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
    public void decode(IByteBuffer _o_) {
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

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BKick.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BKick.Data)_o_;
        if (_linksid != _b_._linksid)
            return false;
        if (_code != _b_._code)
            return false;
        if (!_desc.equals(_b_._desc))
            return false;
        if (_control != _b_._control)
            return false;
        return true;
    }
}
}
