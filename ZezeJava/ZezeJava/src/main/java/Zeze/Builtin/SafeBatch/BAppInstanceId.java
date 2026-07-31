// auto-generated @formatter:off
package Zeze.Builtin.SafeBatch;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BAppInstanceId extends Zeze.Transaction.Bean implements BAppInstanceIdReadOnly {
    public static final long TYPEID = -1598938706937018090L;

    private String _AppInstanceId;

    private static final java.lang.invoke.VarHandle vh_AppInstanceId;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_AppInstanceId = _l_.findVarHandle(BAppInstanceId.class, "_AppInstanceId", String.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getAppInstanceId() {
        if (!isManaged())
            return _AppInstanceId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _AppInstanceId;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.stringValue() : _AppInstanceId;
    }

    public void setAppInstanceId(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _AppInstanceId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_AppInstanceId, _v_));
    }

    @SuppressWarnings("deprecation")
    public BAppInstanceId() {
        _AppInstanceId = "";
    }

    @SuppressWarnings("deprecation")
    public BAppInstanceId(String _AppInstanceId_) {
        if (_AppInstanceId_ == null)
            _AppInstanceId_ = "";
        _AppInstanceId = _AppInstanceId_;
    }

    @Override
    public void reset() {
        setAppInstanceId("");
        _unknown_ = null;
    }

    public void assign(BAppInstanceId _o_) {
        setAppInstanceId(_o_.getAppInstanceId());
        _unknown_ = _o_._unknown_;
    }

    public BAppInstanceId copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAppInstanceId copy() {
        var _c_ = new BAppInstanceId();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BAppInstanceId _a_, BAppInstanceId _b_) {
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
        _s_.append("Zeze.Builtin.SafeBatch.BAppInstanceId: {\n");
        _s_.append(_i1_).append("AppInstanceId=").append(getAppInstanceId()).append('\n');
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
            String _x_ = getAppInstanceId();
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
            setAppInstanceId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BAppInstanceId))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BAppInstanceId)_o_;
        if (!getAppInstanceId().equals(_b_.getAppInstanceId()))
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
                case 1: _AppInstanceId = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setAppInstanceId(_r_.getString(_pn_ + "AppInstanceId"));
        if (getAppInstanceId() == null)
            setAppInstanceId("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "AppInstanceId", getAppInstanceId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "AppInstanceId", "string", "", ""));
        return _v_;
    }
}
