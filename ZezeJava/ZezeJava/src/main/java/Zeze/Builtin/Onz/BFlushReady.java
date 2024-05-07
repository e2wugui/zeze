// auto-generated @formatter:off
package Zeze.Builtin.Onz;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// Flush阶段控制协议
@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BFlushReady extends Zeze.Transaction.Bean implements BFlushReadyReadOnly {
    public static final long TYPEID = 774144301369122476L;

    private long _OnzTid;

    @Override
    public long getOnzTid() {
        if (!isManaged())
            return _OnzTid;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _OnzTid;
        var log = (Log__OnzTid)txn.getLog(objectId() + 1);
        return log != null ? log.value : _OnzTid;
    }

    public void setOnzTid(long value) {
        if (!isManaged()) {
            _OnzTid = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__OnzTid(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BFlushReady() {
    }

    @SuppressWarnings("deprecation")
    public BFlushReady(long _OnzTid_) {
        _OnzTid = _OnzTid_;
    }

    @Override
    public void reset() {
        setOnzTid(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Onz.BFlushReady.Data toData() {
        var data = new Zeze.Builtin.Onz.BFlushReady.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Onz.BFlushReady.Data)other);
    }

    public void assign(BFlushReady.Data other) {
        setOnzTid(other._OnzTid);
        _unknown_ = null;
    }

    public void assign(BFlushReady other) {
        setOnzTid(other.getOnzTid());
        _unknown_ = other._unknown_;
    }

    public BFlushReady copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BFlushReady copy() {
        var copy = new BFlushReady();
        copy.assign(this);
        return copy;
    }

    public static void swap(BFlushReady a, BFlushReady b) {
        BFlushReady save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__OnzTid extends Zeze.Transaction.Logs.LogLong {
        public Log__OnzTid(BFlushReady bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BFlushReady)getBelong())._OnzTid = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Onz.BFlushReady: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("OnzTid=").append(getOnzTid()).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
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
        if (!(_o_ instanceof BFlushReady))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BFlushReady)_o_;
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
    public void followerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _OnzTid = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setOnzTid(rs.getLong(_parents_name_ + "OnzTid"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "OnzTid", getOnzTid());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "OnzTid", "long", "", ""));
        return vars;
    }

// Flush阶段控制协议
@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 774144301369122476L;

    private long _OnzTid;

    public long getOnzTid() {
        return _OnzTid;
    }

    public void setOnzTid(long value) {
        _OnzTid = value;
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
    public Zeze.Builtin.Onz.BFlushReady toBean() {
        var bean = new Zeze.Builtin.Onz.BFlushReady();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BFlushReady)other);
    }

    public void assign(BFlushReady other) {
        _OnzTid = other.getOnzTid();
    }

    public void assign(BFlushReady.Data other) {
        _OnzTid = other._OnzTid;
    }

    @Override
    public BFlushReady.Data copy() {
        var copy = new BFlushReady.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BFlushReady.Data a, BFlushReady.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BFlushReady.Data clone() {
        return (BFlushReady.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Onz.BFlushReady: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("OnzTid=").append(_OnzTid).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
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
