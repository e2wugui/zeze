// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BCommitService extends Zeze.Transaction.Bean implements BCommitServiceReadOnly {
    public static final long TYPEID = -31402640502625825L;

    private String _ServiceName;
    private String _VersionNo;

    @Override
    public String getServiceName() {
        if (!isManaged())
            return _ServiceName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ServiceName;
        var log = (Log__ServiceName)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _ServiceName;
    }

    public void setServiceName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ServiceName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__ServiceName(this, 1, _v_));
    }

    @Override
    public String getVersionNo() {
        if (!isManaged())
            return _VersionNo;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _VersionNo;
        var log = (Log__VersionNo)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _VersionNo;
    }

    public void setVersionNo(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _VersionNo = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__VersionNo(this, 2, _v_));
    }

    @SuppressWarnings("deprecation")
    public BCommitService() {
        _ServiceName = "";
        _VersionNo = "";
    }

    @SuppressWarnings("deprecation")
    public BCommitService(String _ServiceName_, String _VersionNo_) {
        if (_ServiceName_ == null)
            _ServiceName_ = "";
        _ServiceName = _ServiceName_;
        if (_VersionNo_ == null)
            _VersionNo_ = "";
        _VersionNo = _VersionNo_;
    }

    @Override
    public void reset() {
        setServiceName("");
        setVersionNo("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Zoker.BCommitService.Data toData() {
        var _d_ = new Zeze.Builtin.Zoker.BCommitService.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Zoker.BCommitService.Data)_o_);
    }

    public void assign(BCommitService.Data _o_) {
        setServiceName(_o_._ServiceName);
        setVersionNo(_o_._VersionNo);
        _unknown_ = null;
    }

    public void assign(BCommitService _o_) {
        setServiceName(_o_.getServiceName());
        setVersionNo(_o_.getVersionNo());
        _unknown_ = _o_._unknown_;
    }

    public BCommitService copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCommitService copy() {
        var _c_ = new BCommitService();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BCommitService _a_, BCommitService _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ServiceName extends Zeze.Transaction.Logs.LogString {
        public Log__ServiceName(BCommitService _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BCommitService)getBelong())._ServiceName = value; }
    }

    private static final class Log__VersionNo extends Zeze.Transaction.Logs.LogString {
        public Log__VersionNo(BCommitService _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BCommitService)getBelong())._VersionNo = value; }
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Zoker.BCommitService: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("ServiceName=").append(getServiceName()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("VersionNo=").append(getVersionNo()).append(System.lineSeparator());
        _l_ -= 4;
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
            String _x_ = getServiceName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getVersionNo();
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
            setServiceName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setVersionNo(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BCommitService))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BCommitService)_o_;
        if (!getServiceName().equals(_b_.getServiceName()))
            return false;
        if (!getVersionNo().equals(_b_.getVersionNo()))
            return false;
        return true;
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
                case 1: _ServiceName = _v_.stringValue(); break;
                case 2: _VersionNo = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setServiceName(_r_.getString(_pn_ + "ServiceName"));
        if (getServiceName() == null)
            setServiceName("");
        setVersionNo(_r_.getString(_pn_ + "VersionNo"));
        if (getVersionNo() == null)
            setVersionNo("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "ServiceName", getServiceName());
        _s_.appendString(_pn_ + "VersionNo", getVersionNo());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ServiceName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "VersionNo", "string", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -31402640502625825L;

    private String _ServiceName;
    private String _VersionNo;

    public String getServiceName() {
        return _ServiceName;
    }

    public void setServiceName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _ServiceName = _v_;
    }

    public String getVersionNo() {
        return _VersionNo;
    }

    public void setVersionNo(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _VersionNo = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _ServiceName = "";
        _VersionNo = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _ServiceName_, String _VersionNo_) {
        if (_ServiceName_ == null)
            _ServiceName_ = "";
        _ServiceName = _ServiceName_;
        if (_VersionNo_ == null)
            _VersionNo_ = "";
        _VersionNo = _VersionNo_;
    }

    @Override
    public void reset() {
        _ServiceName = "";
        _VersionNo = "";
    }

    @Override
    public Zeze.Builtin.Zoker.BCommitService toBean() {
        var _b_ = new Zeze.Builtin.Zoker.BCommitService();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BCommitService)_o_);
    }

    public void assign(BCommitService _o_) {
        _ServiceName = _o_.getServiceName();
        _VersionNo = _o_.getVersionNo();
    }

    public void assign(BCommitService.Data _o_) {
        _ServiceName = _o_._ServiceName;
        _VersionNo = _o_._VersionNo;
    }

    @Override
    public BCommitService.Data copy() {
        var _c_ = new BCommitService.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BCommitService.Data _a_, BCommitService.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BCommitService.Data clone() {
        return (BCommitService.Data)super.clone();
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Zoker.BCommitService: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("ServiceName=").append(_ServiceName).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("VersionNo=").append(_VersionNo).append(System.lineSeparator());
        _l_ -= 4;
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
            String _x_ = _ServiceName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = _VersionNo;
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
            _ServiceName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _VersionNo = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
