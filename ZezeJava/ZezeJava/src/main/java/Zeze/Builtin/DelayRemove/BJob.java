// auto-generated @formatter:off
package Zeze.Builtin.DelayRemove;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BJob extends Zeze.Transaction.Bean implements BJobReadOnly {
    public static final long TYPEID = -489344497836886892L;

    private String _JobHandleName;
    private Zeze.Net.Binary _JobState;

    private transient Object __zeze_map_key__;

    @Override
    public Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object _v_) {
        __zeze_map_key__ = _v_;
    }

    private static final java.lang.invoke.VarHandle vh_JobHandleName;
    private static final java.lang.invoke.VarHandle vh_JobState;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_JobHandleName = _l_.findVarHandle(BJob.class, "_JobHandleName", String.class);
            vh_JobState = _l_.findVarHandle(BJob.class, "_JobState", Zeze.Net.Binary.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getJobHandleName() {
        if (!isManaged())
            return _JobHandleName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _JobHandleName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _JobHandleName;
    }

    public void setJobHandleName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _JobHandleName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_JobHandleName, _v_));
    }

    @Override
    public Zeze.Net.Binary getJobState() {
        if (!isManaged())
            return _JobState;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _JobState;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _JobState;
    }

    public void setJobState(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _JobState = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 2, vh_JobState, _v_));
    }

    @SuppressWarnings("deprecation")
    public BJob() {
        _JobHandleName = "";
        _JobState = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BJob(String _JobHandleName_, Zeze.Net.Binary _JobState_) {
        if (_JobHandleName_ == null)
            _JobHandleName_ = "";
        _JobHandleName = _JobHandleName_;
        if (_JobState_ == null)
            _JobState_ = Zeze.Net.Binary.Empty;
        _JobState = _JobState_;
    }

    @Override
    public void reset() {
        setJobHandleName("");
        setJobState(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    public void assign(BJob _o_) {
        setJobHandleName(_o_.getJobHandleName());
        setJobState(_o_.getJobState());
        _unknown_ = _o_._unknown_;
    }

    public BJob copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BJob copy() {
        var _c_ = new BJob();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BJob _a_, BJob _b_) {
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
        _s_.append("Zeze.Builtin.DelayRemove.BJob: {\n");
        _s_.append(_i1_).append("JobHandleName=").append(getJobHandleName()).append(",\n");
        _s_.append(_i1_).append("JobState=").append(getJobState()).append('\n');
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
            String _x_ = getJobHandleName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getJobState();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
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
            setJobHandleName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setJobState(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BJob))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BJob)_o_;
        if (!getJobHandleName().equals(_b_.getJobHandleName()))
            return false;
        if (!getJobState().equals(_b_.getJobState()))
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
                case 1: _JobHandleName = _v_.stringValue(); break;
                case 2: _JobState = _v_.binaryValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setJobHandleName(_r_.getString(_pn_ + "JobHandleName"));
        if (getJobHandleName() == null)
            setJobHandleName("");
        setJobState(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "JobState")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "JobHandleName", getJobHandleName());
        _s_.appendBinary(_pn_ + "JobState", getJobState());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "JobHandleName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "JobState", "binary", "", ""));
        return _v_;
    }
}
