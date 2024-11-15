// auto-generated @formatter:off
package Zeze.Builtin.MQ;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BOptions extends Zeze.Transaction.Bean implements BOptionsReadOnly {
    public static final long TYPEID = 7171781613253093556L;

    public static final int GlobalOne = 1;
    public static final int MultiInstance = 2; // MQ 类型
    public static final int Single = 4;
    public static final int DoubleWrite = 8;
    public static final int Raft3 = 16; // ReadOptions 消息派发模式
    public static final int ReadOneByOne = 32;
    public static final int ReadConcurrent = 64; // WriteOptions 消息保存模式
    public static final int WriteFileSync = 128;
    public static final int WriteFileSystem = 256;

    private int _Options;

    private static final java.lang.invoke.VarHandle vh_Options;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Options = _l_.findVarHandle(BOptions.class, "_Options", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getOptions() {
        if (!isManaged())
            return _Options;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Options;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Options;
    }

    public void setOptions(int _v_) {
        if (!isManaged()) {
            _Options = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_Options, _v_));
    }

    @SuppressWarnings("deprecation")
    public BOptions() {
    }

    @SuppressWarnings("deprecation")
    public BOptions(int _Options_) {
        _Options = _Options_;
    }

    @Override
    public void reset() {
        setOptions(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.MQ.BOptions.Data toData() {
        var _d_ = new Zeze.Builtin.MQ.BOptions.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.MQ.BOptions.Data)_o_);
    }

    public void assign(BOptions.Data _o_) {
        setOptions(_o_._Options);
        _unknown_ = null;
    }

    public void assign(BOptions _o_) {
        setOptions(_o_.getOptions());
        _unknown_ = _o_._unknown_;
    }

    public BOptions copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BOptions copy() {
        var _c_ = new BOptions();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BOptions _a_, BOptions _b_) {
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
        _s_.append("Zeze.Builtin.MQ.BOptions: {\n");
        _s_.append(_i1_).append("Options=").append(getOptions()).append('\n');
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
            int _x_ = getOptions();
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
            setOptions(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BOptions))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BOptions)_o_;
        if (getOptions() != _b_.getOptions())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getOptions() < 0)
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
                case 1: _Options = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setOptions(_r_.getInt(_pn_ + "Options"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "Options", getOptions());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Options", "int", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 7171781613253093556L;

    public static final int GlobalOne = 1;
    public static final int MultiInstance = 2; // MQ 类型
    public static final int Single = 4;
    public static final int DoubleWrite = 8;
    public static final int Raft3 = 16; // ReadOptions 消息派发模式
    public static final int ReadOneByOne = 32;
    public static final int ReadConcurrent = 64; // WriteOptions 消息保存模式
    public static final int WriteFileSync = 128;
    public static final int WriteFileSystem = 256;

    private int _Options;

    public int getOptions() {
        return _Options;
    }

    public void setOptions(int _v_) {
        _Options = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(int _Options_) {
        _Options = _Options_;
    }

    @Override
    public void reset() {
        _Options = 0;
    }

    @Override
    public Zeze.Builtin.MQ.BOptions toBean() {
        var _b_ = new Zeze.Builtin.MQ.BOptions();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BOptions)_o_);
    }

    public void assign(BOptions _o_) {
        _Options = _o_.getOptions();
    }

    public void assign(BOptions.Data _o_) {
        _Options = _o_._Options;
    }

    @Override
    public BOptions.Data copy() {
        var _c_ = new BOptions.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BOptions.Data _a_, BOptions.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BOptions.Data clone() {
        return (BOptions.Data)super.clone();
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
        _s_.append("Zeze.Builtin.MQ.BOptions: {\n");
        _s_.append(_i1_).append("Options=").append(_Options).append('\n');
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
            int _x_ = _Options;
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
            _Options = _o_.ReadInt(_t_);
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
        if (!(_o_ instanceof BOptions.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BOptions.Data)_o_;
        if (_Options != _b_._Options)
            return false;
        return true;
    }
}
}
