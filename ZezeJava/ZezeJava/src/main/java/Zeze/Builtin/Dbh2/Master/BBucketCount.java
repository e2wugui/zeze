// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BBucketCount extends Zeze.Transaction.Bean implements BBucketCountReadOnly {
    public static final long TYPEID = 2147869342393134029L;

    private int _Count;

    @Override
    public int getCount() {
        if (!isManaged())
            return _Count;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Count;
        var log = (Log__Count)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Count;
    }

    public void setCount(int _v_) {
        if (!isManaged()) {
            _Count = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Count(this, 1, _v_));
    }

    @SuppressWarnings("deprecation")
    public BBucketCount() {
    }

    @SuppressWarnings("deprecation")
    public BBucketCount(int _Count_) {
        _Count = _Count_;
    }

    @Override
    public void reset() {
        setCount(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BBucketCount.Data toData() {
        var _d_ = new Zeze.Builtin.Dbh2.Master.BBucketCount.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Dbh2.Master.BBucketCount.Data)_o_);
    }

    public void assign(BBucketCount.Data _o_) {
        setCount(_o_._Count);
        _unknown_ = null;
    }

    public void assign(BBucketCount _o_) {
        setCount(_o_.getCount());
        _unknown_ = _o_._unknown_;
    }

    public BBucketCount copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BBucketCount copy() {
        var _c_ = new BBucketCount();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BBucketCount _a_, BBucketCount _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Count extends Zeze.Transaction.Logs.LogInt {
        public Log__Count(BBucketCount _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BBucketCount)getBelong())._Count = value; }
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
        _s_.append("Zeze.Builtin.Dbh2.Master.BBucketCount: {\n");
        _s_.append(_i1_).append("Count=").append(getCount()).append('\n');
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
            int _x_ = getCount();
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
            setCount(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BBucketCount))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BBucketCount)_o_;
        if (getCount() != _b_.getCount())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getCount() < 0)
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
                case 1: _Count = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setCount(_r_.getInt(_pn_ + "Count"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "Count", getCount());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Count", "int", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 2147869342393134029L;

    private int _Count;

    public int getCount() {
        return _Count;
    }

    public void setCount(int _v_) {
        _Count = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(int _Count_) {
        _Count = _Count_;
    }

    @Override
    public void reset() {
        _Count = 0;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BBucketCount toBean() {
        var _b_ = new Zeze.Builtin.Dbh2.Master.BBucketCount();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BBucketCount)_o_);
    }

    public void assign(BBucketCount _o_) {
        _Count = _o_.getCount();
    }

    public void assign(BBucketCount.Data _o_) {
        _Count = _o_._Count;
    }

    @Override
    public BBucketCount.Data copy() {
        var _c_ = new BBucketCount.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BBucketCount.Data _a_, BBucketCount.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BBucketCount.Data clone() {
        return (BBucketCount.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Dbh2.Master.BBucketCount: {\n");
        _s_.append(_i1_).append("Count=").append(_Count).append('\n');
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
            int _x_ = _Count;
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
            _Count = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
