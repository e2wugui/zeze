// auto-generated @formatter:off
package Zeze.Builtin.LoginQueue;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BQueueSize extends Zeze.Transaction.Bean implements BQueueSizeReadOnly {
    public static final long TYPEID = -3671805030586633468L;

    private int _QueueSize;

    private static final java.lang.invoke.VarHandle vh_QueueSize;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_QueueSize = _l_.findVarHandle(BQueueSize.class, "_QueueSize", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getQueueSize() {
        if (!isManaged())
            return _QueueSize;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _QueueSize;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _QueueSize;
    }

    public void setQueueSize(int _v_) {
        if (!isManaged()) {
            _QueueSize = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_QueueSize, _v_));
    }

    @SuppressWarnings("deprecation")
    public BQueueSize() {
    }

    @SuppressWarnings("deprecation")
    public BQueueSize(int _QueueSize_) {
        _QueueSize = _QueueSize_;
    }

    @Override
    public void reset() {
        setQueueSize(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.LoginQueue.BQueueSize.Data toData() {
        var _d_ = new Zeze.Builtin.LoginQueue.BQueueSize.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.LoginQueue.BQueueSize.Data)_o_);
    }

    public void assign(BQueueSize.Data _o_) {
        setQueueSize(_o_._QueueSize);
        _unknown_ = null;
    }

    public void assign(BQueueSize _o_) {
        setQueueSize(_o_.getQueueSize());
        _unknown_ = _o_._unknown_;
    }

    public BQueueSize copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BQueueSize copy() {
        var _c_ = new BQueueSize();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BQueueSize _a_, BQueueSize _b_) {
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
        _s_.append("Zeze.Builtin.LoginQueue.BQueueSize: {\n");
        _s_.append(_i1_).append("QueueSize=").append(getQueueSize()).append('\n');
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
            int _x_ = getQueueSize();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
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
            setQueueSize(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BQueueSize))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BQueueSize)_o_;
        if (getQueueSize() != _b_.getQueueSize())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getQueueSize() < 0)
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
                case 1: _QueueSize = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setQueueSize(_r_.getInt(_pn_ + "QueueSize"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "QueueSize", getQueueSize());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "QueueSize", "int", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -3671805030586633468L;

    private int _QueueSize;

    public int getQueueSize() {
        return _QueueSize;
    }

    public void setQueueSize(int _v_) {
        _QueueSize = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(int _QueueSize_) {
        _QueueSize = _QueueSize_;
    }

    @Override
    public void reset() {
        _QueueSize = 0;
    }

    @Override
    public Zeze.Builtin.LoginQueue.BQueueSize toBean() {
        var _b_ = new Zeze.Builtin.LoginQueue.BQueueSize();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BQueueSize)_o_);
    }

    public void assign(BQueueSize _o_) {
        _QueueSize = _o_.getQueueSize();
    }

    public void assign(BQueueSize.Data _o_) {
        _QueueSize = _o_._QueueSize;
    }

    @Override
    public BQueueSize.Data copy() {
        var _c_ = new BQueueSize.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BQueueSize.Data _a_, BQueueSize.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BQueueSize.Data clone() {
        return (BQueueSize.Data)super.clone();
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
        _s_.append("Zeze.Builtin.LoginQueue.BQueueSize: {\n");
        _s_.append(_i1_).append("QueueSize=").append(_QueueSize).append('\n');
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
            int _x_ = _QueueSize;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
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
            _QueueSize = _o_.ReadInt(_t_);
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
        if (!(_o_ instanceof BQueueSize.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BQueueSize.Data)_o_;
        if (_QueueSize != _b_._QueueSize)
            return false;
        return true;
    }
}
}
