// auto-generated @formatter:off
package TaskTest.TaskExt;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

// 扩展的任务 Extended Task
@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BMyTask01 extends Zeze.Transaction.Bean implements BMyTask01ReadOnly {
    public static final long TYPEID = -3524416178202009108L;

    private long _placeholder;
    private int _taskNum;

    private static final java.lang.invoke.VarHandle vh_placeholder;
    private static final java.lang.invoke.VarHandle vh_taskNum;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_placeholder = _l_.findVarHandle(BMyTask01.class, "_placeholder", long.class);
            vh_taskNum = _l_.findVarHandle(BMyTask01.class, "_taskNum", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getPlaceholder() {
        if (!isManaged())
            return _placeholder;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _placeholder;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _placeholder;
    }

    public void setPlaceholder(long _v_) {
        if (!isManaged()) {
            _placeholder = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_placeholder, _v_));
    }

    @Override
    public int getTaskNum() {
        if (!isManaged())
            return _taskNum;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _taskNum;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _taskNum;
    }

    public void setTaskNum(int _v_) {
        if (!isManaged()) {
            _taskNum = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 2, vh_taskNum, _v_));
    }

    @SuppressWarnings("deprecation")
    public BMyTask01() {
    }

    @SuppressWarnings("deprecation")
    public BMyTask01(long _placeholder_, int _taskNum_) {
        _placeholder = _placeholder_;
        _taskNum = _taskNum_;
    }

    @Override
    public void reset() {
        setPlaceholder(0);
        setTaskNum(0);
        _unknown_ = null;
    }

    public void assign(BMyTask01 _o_) {
        setPlaceholder(_o_.getPlaceholder());
        setTaskNum(_o_.getTaskNum());
        _unknown_ = _o_._unknown_;
    }

    public BMyTask01 copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BMyTask01 copy() {
        var _c_ = new BMyTask01();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BMyTask01 _a_, BMyTask01 _b_) {
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
        _s_.append("TaskTest.TaskExt.BMyTask01: {\n");
        _s_.append(_i1_).append("placeholder=").append(getPlaceholder()).append(",\n");
        _s_.append(_i1_).append("taskNum=").append(getTaskNum()).append('\n');
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

    private byte @Nullable [] _unknown_;

    public byte @Nullable [] unknown() {
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
            long _x_ = getPlaceholder();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getTaskNum();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            setPlaceholder(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setTaskNum(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BMyTask01))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BMyTask01)_o_;
        if (getPlaceholder() != _b_.getPlaceholder())
            return false;
        if (getTaskNum() != _b_.getTaskNum())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getPlaceholder() < 0)
            return true;
        if (getTaskNum() < 0)
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
                case 1: _placeholder = _v_.longValue(); break;
                case 2: _taskNum = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setPlaceholder(_r_.getLong(_pn_ + "placeholder"));
        setTaskNum(_r_.getInt(_pn_ + "taskNum"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "placeholder", getPlaceholder());
        _s_.appendInt(_pn_ + "taskNum", getTaskNum());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "placeholder", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "taskNum", "int", "", ""));
        return _v_;
    }
}
