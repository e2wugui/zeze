// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BNotify extends Zeze.Transaction.Bean implements BNotifyReadOnly {
    public static final long TYPEID = 663625160021568926L;

    private Zeze.Net.Binary _FullEncodedProtocol;

    @Override
    public Zeze.Net.Binary getFullEncodedProtocol() {
        if (!isManaged())
            return _FullEncodedProtocol;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _FullEncodedProtocol;
        var log = (Log__FullEncodedProtocol)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _FullEncodedProtocol;
    }

    public void setFullEncodedProtocol(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _FullEncodedProtocol = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__FullEncodedProtocol(this, 1, _v_));
    }

    @SuppressWarnings("deprecation")
    public BNotify() {
        _FullEncodedProtocol = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BNotify(Zeze.Net.Binary _FullEncodedProtocol_) {
        if (_FullEncodedProtocol_ == null)
            _FullEncodedProtocol_ = Zeze.Net.Binary.Empty;
        _FullEncodedProtocol = _FullEncodedProtocol_;
    }

    @Override
    public void reset() {
        setFullEncodedProtocol(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    public void assign(BNotify _o_) {
        setFullEncodedProtocol(_o_.getFullEncodedProtocol());
        _unknown_ = _o_._unknown_;
    }

    public BNotify copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BNotify copy() {
        var _c_ = new BNotify();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BNotify _a_, BNotify _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__FullEncodedProtocol extends Zeze.Transaction.Logs.LogBinary {
        public Log__FullEncodedProtocol(BNotify _b_, int _i_, Zeze.Net.Binary _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BNotify)getBelong())._FullEncodedProtocol = value; }
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Game.Online.BNotify: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("FullEncodedProtocol=").append(getFullEncodedProtocol()).append(System.lineSeparator());
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
            var _x_ = getFullEncodedProtocol();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
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
            setFullEncodedProtocol(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BNotify))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BNotify)_o_;
        if (!getFullEncodedProtocol().equals(_b_.getFullEncodedProtocol()))
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
                case 1: _FullEncodedProtocol = _v_.binaryValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setFullEncodedProtocol(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "FullEncodedProtocol")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendBinary(_pn_ + "FullEncodedProtocol", getFullEncodedProtocol());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "FullEncodedProtocol", "binary", "", ""));
        return _v_;
    }
}
