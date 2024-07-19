// auto-generated @formatter:off
package Zeze.Builtin.Onz;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// 2段提交相关控制协议
@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BCommit extends Zeze.Transaction.Bean implements BCommitReadOnly {
    public static final long TYPEID = 835137953772665828L;

    private long _OnzTid;

    @Override
    public long getOnzTid() {
        if (!isManaged())
            return _OnzTid;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _OnzTid;
        var log = (Log__OnzTid)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _OnzTid;
    }

    public void setOnzTid(long _v_) {
        if (!isManaged()) {
            _OnzTid = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__OnzTid(this, 1, _v_));
    }

    @SuppressWarnings("deprecation")
    public BCommit() {
    }

    @SuppressWarnings("deprecation")
    public BCommit(long _OnzTid_) {
        _OnzTid = _OnzTid_;
    }

    @Override
    public void reset() {
        setOnzTid(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Onz.BCommit.Data toData() {
        var _d_ = new Zeze.Builtin.Onz.BCommit.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Onz.BCommit.Data)_o_);
    }

    public void assign(BCommit.Data _o_) {
        setOnzTid(_o_._OnzTid);
        _unknown_ = null;
    }

    public void assign(BCommit _o_) {
        setOnzTid(_o_.getOnzTid());
        _unknown_ = _o_._unknown_;
    }

    public BCommit copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCommit copy() {
        var _c_ = new BCommit();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BCommit _a_, BCommit _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__OnzTid extends Zeze.Transaction.Logs.LogLong {
        public Log__OnzTid(BCommit _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BCommit)getBelong())._OnzTid = value; }
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Onz.BCommit: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("OnzTid=").append(getOnzTid()).append(System.lineSeparator());
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
            long _x_ = getOnzTid();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
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
            setOnzTid(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BCommit))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BCommit)_o_;
        if (getOnzTid() != _b_.getOnzTid())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getOnzTid() < 0)
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
                case 1: _OnzTid = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setOnzTid(_r_.getLong(_pn_ + "OnzTid"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "OnzTid", getOnzTid());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "OnzTid", "long", "", ""));
        return _v_;
    }

// 2段提交相关控制协议
@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 835137953772665828L;

    private long _OnzTid;

    public long getOnzTid() {
        return _OnzTid;
    }

    public void setOnzTid(long _v_) {
        _OnzTid = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(long _OnzTid_) {
        _OnzTid = _OnzTid_;
    }

    @Override
    public void reset() {
        _OnzTid = 0;
    }

    @Override
    public Zeze.Builtin.Onz.BCommit toBean() {
        var _b_ = new Zeze.Builtin.Onz.BCommit();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BCommit)_o_);
    }

    public void assign(BCommit _o_) {
        _OnzTid = _o_.getOnzTid();
    }

    public void assign(BCommit.Data _o_) {
        _OnzTid = _o_._OnzTid;
    }

    @Override
    public BCommit.Data copy() {
        var _c_ = new BCommit.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BCommit.Data _a_, BCommit.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BCommit.Data clone() {
        return (BCommit.Data)super.clone();
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Onz.BCommit: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("OnzTid=").append(_OnzTid).append(System.lineSeparator());
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
            long _x_ = _OnzTid;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _OnzTid = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
