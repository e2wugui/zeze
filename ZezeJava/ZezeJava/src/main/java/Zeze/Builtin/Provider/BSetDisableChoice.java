// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BSetDisableChoice extends Zeze.Transaction.Bean implements BSetDisableChoiceReadOnly {
    public static final long TYPEID = -1885417062233949302L;

    private boolean _DisableChoice;

    private static final java.lang.invoke.VarHandle vh_DisableChoice;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_DisableChoice = _l_.findVarHandle(BSetDisableChoice.class, "_DisableChoice", boolean.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public boolean isDisableChoice() {
        if (!isManaged())
            return _DisableChoice;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _DisableChoice;
        var log = (Zeze.Transaction.Logs.LogBool)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _DisableChoice;
    }

    public void setDisableChoice(boolean _v_) {
        if (!isManaged()) {
            _DisableChoice = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBool(this, 1, vh_DisableChoice, _v_));
    }

    @SuppressWarnings("deprecation")
    public BSetDisableChoice() {
    }

    @SuppressWarnings("deprecation")
    public BSetDisableChoice(boolean _DisableChoice_) {
        _DisableChoice = _DisableChoice_;
    }

    @Override
    public void reset() {
        setDisableChoice(false);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Provider.BSetDisableChoice.Data toData() {
        var _d_ = new Zeze.Builtin.Provider.BSetDisableChoice.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Provider.BSetDisableChoice.Data)_o_);
    }

    public void assign(BSetDisableChoice.Data _o_) {
        setDisableChoice(_o_._DisableChoice);
        _unknown_ = null;
    }

    public void assign(BSetDisableChoice _o_) {
        setDisableChoice(_o_.isDisableChoice());
        _unknown_ = _o_._unknown_;
    }

    public BSetDisableChoice copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSetDisableChoice copy() {
        var _c_ = new BSetDisableChoice();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSetDisableChoice _a_, BSetDisableChoice _b_) {
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
        _s_.append("Zeze.Builtin.Provider.BSetDisableChoice: {\n");
        _s_.append(_i1_).append("DisableChoice=").append(isDisableChoice()).append('\n');
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
            boolean _x_ = isDisableChoice();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
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
            setDisableChoice(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BSetDisableChoice))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSetDisableChoice)_o_;
        if (isDisableChoice() != _b_.isDisableChoice())
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
                case 1: _DisableChoice = _v_.booleanValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setDisableChoice(_r_.getBoolean(_pn_ + "DisableChoice"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendBoolean(_pn_ + "DisableChoice", isDisableChoice());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "DisableChoice", "bool", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -1885417062233949302L;

    private boolean _DisableChoice;

    public boolean isDisableChoice() {
        return _DisableChoice;
    }

    public void setDisableChoice(boolean _v_) {
        _DisableChoice = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(boolean _DisableChoice_) {
        _DisableChoice = _DisableChoice_;
    }

    @Override
    public void reset() {
        _DisableChoice = false;
    }

    @Override
    public Zeze.Builtin.Provider.BSetDisableChoice toBean() {
        var _b_ = new Zeze.Builtin.Provider.BSetDisableChoice();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BSetDisableChoice)_o_);
    }

    public void assign(BSetDisableChoice _o_) {
        _DisableChoice = _o_.isDisableChoice();
    }

    public void assign(BSetDisableChoice.Data _o_) {
        _DisableChoice = _o_._DisableChoice;
    }

    @Override
    public BSetDisableChoice.Data copy() {
        var _c_ = new BSetDisableChoice.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSetDisableChoice.Data _a_, BSetDisableChoice.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BSetDisableChoice.Data clone() {
        return (BSetDisableChoice.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Provider.BSetDisableChoice: {\n");
        _s_.append(_i1_).append("DisableChoice=").append(_DisableChoice).append('\n');
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
            boolean _x_ = _DisableChoice;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _DisableChoice = _o_.ReadBool(_t_);
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
        if (!(_o_ instanceof BSetDisableChoice.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSetDisableChoice.Data)_o_;
        if (_DisableChoice != _b_._DisableChoice)
            return false;
        return true;
    }
}
}
