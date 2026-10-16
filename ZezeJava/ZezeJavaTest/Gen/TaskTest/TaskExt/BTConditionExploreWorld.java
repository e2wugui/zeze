// auto-generated @formatter:off
package TaskTest.TaskExt;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

// 扩展的条件 Extended Condition
@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BTConditionExploreWorld extends Zeze.Transaction.Bean implements BTConditionExploreWorldReadOnly {
    public static final long TYPEID = 5234468502297408856L;

    private double _exploreRate; // 大地图探索率：0 - 100
    private boolean _finished;

    private static final java.lang.invoke.VarHandle vh_exploreRate;
    private static final java.lang.invoke.VarHandle vh_finished;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_exploreRate = _l_.findVarHandle(BTConditionExploreWorld.class, "_exploreRate", double.class);
            vh_finished = _l_.findVarHandle(BTConditionExploreWorld.class, "_finished", boolean.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public double getExploreRate() {
        if (!isManaged())
            return _exploreRate;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _exploreRate;
        var log = (Zeze.Transaction.Logs.LogDouble)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _exploreRate;
    }

    public void setExploreRate(double _v_) {
        if (!isManaged()) {
            _exploreRate = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogDouble(this, 1, vh_exploreRate, _v_));
    }

    @Override
    public boolean isFinished() {
        if (!isManaged())
            return _finished;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _finished;
        var log = (Zeze.Transaction.Logs.LogBool)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _finished;
    }

    public void setFinished(boolean _v_) {
        if (!isManaged()) {
            _finished = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBool(this, 2, vh_finished, _v_));
    }

    @SuppressWarnings("deprecation")
    public BTConditionExploreWorld() {
    }

    @SuppressWarnings("deprecation")
    public BTConditionExploreWorld(double _exploreRate_, boolean _finished_) {
        _exploreRate = _exploreRate_;
        _finished = _finished_;
    }

    @Override
    public void reset() {
        setExploreRate(0);
        setFinished(false);
        _unknown_ = null;
    }

    public void assign(BTConditionExploreWorld _o_) {
        setExploreRate(_o_.getExploreRate());
        setFinished(_o_.isFinished());
        _unknown_ = _o_._unknown_;
    }

    public BTConditionExploreWorld copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTConditionExploreWorld copy() {
        var _c_ = new BTConditionExploreWorld();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTConditionExploreWorld _a_, BTConditionExploreWorld _b_) {
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
        _s_.append("TaskTest.TaskExt.BTConditionExploreWorld: {\n");
        _s_.append(_i1_).append("exploreRate=").append(getExploreRate()).append(",\n");
        _s_.append(_i1_).append("finished=").append(isFinished()).append('\n');
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
            double _x_ = getExploreRate();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.DOUBLE);
                _o_.WriteDouble(_x_);
            }
        }
        {
            boolean _x_ = isFinished();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
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
            setExploreRate(_o_.ReadDouble(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setFinished(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BTConditionExploreWorld))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTConditionExploreWorld)_o_;
        if (getExploreRate() != _b_.getExploreRate())
            return false;
        if (isFinished() != _b_.isFinished())
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
                case 1: _exploreRate = _v_.doubleValue(); break;
                case 2: _finished = _v_.booleanValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setExploreRate(_r_.getDouble(_pn_ + "exploreRate"));
        setFinished(_r_.getBoolean(_pn_ + "finished"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendDouble(_pn_ + "exploreRate", getExploreRate());
        _s_.appendBoolean(_pn_ + "finished", isFinished());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "exploreRate", "double", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "finished", "bool", "", ""));
        return _v_;
    }
}
