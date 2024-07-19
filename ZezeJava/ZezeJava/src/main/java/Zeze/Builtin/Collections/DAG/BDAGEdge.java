// auto-generated @formatter:off
package Zeze.Builtin.Collections.DAG;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// 有向图的边类型（如：任务的连接方式）
@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BDAGEdge extends Zeze.Transaction.Bean implements BDAGEdgeReadOnly {
    public static final long TYPEID = -6222763240399548476L;

    private Zeze.Builtin.Collections.DAG.BDAGNodeKey _From; // 有向图中有向边的起点
    private Zeze.Builtin.Collections.DAG.BDAGNodeKey _To; // 有向图中有向边的终点

    @Override
    public Zeze.Builtin.Collections.DAG.BDAGNodeKey getFrom() {
        if (!isManaged())
            return _From;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _From;
        var log = (Log__From)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _From;
    }

    public void setFrom(Zeze.Builtin.Collections.DAG.BDAGNodeKey _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _From = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__From(this, 1, _v_));
    }

    @Override
    public Zeze.Builtin.Collections.DAG.BDAGNodeKey getTo() {
        if (!isManaged())
            return _To;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _To;
        var log = (Log__To)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _To;
    }

    public void setTo(Zeze.Builtin.Collections.DAG.BDAGNodeKey _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _To = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__To(this, 2, _v_));
    }

    @SuppressWarnings("deprecation")
    public BDAGEdge() {
        _From = new Zeze.Builtin.Collections.DAG.BDAGNodeKey();
        _To = new Zeze.Builtin.Collections.DAG.BDAGNodeKey();
    }

    @SuppressWarnings("deprecation")
    public BDAGEdge(Zeze.Builtin.Collections.DAG.BDAGNodeKey _From_, Zeze.Builtin.Collections.DAG.BDAGNodeKey _To_) {
        if (_From_ == null)
            _From_ = new Zeze.Builtin.Collections.DAG.BDAGNodeKey();
        _From = _From_;
        if (_To_ == null)
            _To_ = new Zeze.Builtin.Collections.DAG.BDAGNodeKey();
        _To = _To_;
    }

    @Override
    public void reset() {
        setFrom(new Zeze.Builtin.Collections.DAG.BDAGNodeKey());
        setTo(new Zeze.Builtin.Collections.DAG.BDAGNodeKey());
        _unknown_ = null;
    }

    public void assign(BDAGEdge _o_) {
        setFrom(_o_.getFrom());
        setTo(_o_.getTo());
        _unknown_ = _o_._unknown_;
    }

    public BDAGEdge copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BDAGEdge copy() {
        var _c_ = new BDAGEdge();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BDAGEdge _a_, BDAGEdge _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__From extends Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Collections.DAG.BDAGNodeKey> {
        public Log__From(BDAGEdge _b_, int _i_, Zeze.Builtin.Collections.DAG.BDAGNodeKey _v_) { super(Zeze.Builtin.Collections.DAG.BDAGNodeKey.class, _b_, _i_, _v_); }

        @Override
        public void commit() { ((BDAGEdge)getBelong())._From = value; }
    }

    private static final class Log__To extends Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Collections.DAG.BDAGNodeKey> {
        public Log__To(BDAGEdge _b_, int _i_, Zeze.Builtin.Collections.DAG.BDAGNodeKey _v_) { super(Zeze.Builtin.Collections.DAG.BDAGNodeKey.class, _b_, _i_, _v_); }

        @Override
        public void commit() { ((BDAGEdge)getBelong())._To = value; }
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Collections.DAG.BDAGEdge: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("From=").append(System.lineSeparator());
        getFrom().buildString(_s_, _l_ + 4);
        _s_.append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("To=").append(System.lineSeparator());
        getTo().buildString(_s_, _l_ + 4);
        _s_.append(System.lineSeparator());
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getFrom().encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 2, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getTo().encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
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
            _o_.ReadBean(getFrom(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _o_.ReadBean(getTo(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BDAGEdge))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BDAGEdge)_o_;
        if (!getFrom().equals(_b_.getFrom()))
            return false;
        if (!getTo().equals(_b_.getTo()))
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
                case 1: _From = ((Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Collections.DAG.BDAGNodeKey>)_v_).value; break;
                case 2: _To = ((Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Collections.DAG.BDAGNodeKey>)_v_).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        _p_.add("From");
        getFrom().decodeResultSet(_p_, _r_);
        _p_.remove(_p_.size() - 1);
        _p_.add("To");
        getTo().decodeResultSet(_p_, _r_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        _p_.add("From");
        getFrom().encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
        _p_.add("To");
        getTo().encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "From", "Zeze.Builtin.Collections.DAG.BDAGNodeKey", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "To", "Zeze.Builtin.Collections.DAG.BDAGNodeKey", "", ""));
        return _v_;
    }
}
