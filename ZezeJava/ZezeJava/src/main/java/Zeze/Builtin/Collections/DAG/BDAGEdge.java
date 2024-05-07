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
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _From;
        var log = (Log__From)txn.getLog(objectId() + 1);
        return log != null ? log.value : _From;
    }

    public void setFrom(Zeze.Builtin.Collections.DAG.BDAGNodeKey value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _From = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__From(this, 1, value));
    }

    @Override
    public Zeze.Builtin.Collections.DAG.BDAGNodeKey getTo() {
        if (!isManaged())
            return _To;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _To;
        var log = (Log__To)txn.getLog(objectId() + 2);
        return log != null ? log.value : _To;
    }

    public void setTo(Zeze.Builtin.Collections.DAG.BDAGNodeKey value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _To = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__To(this, 2, value));
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

    public void assign(BDAGEdge other) {
        setFrom(other.getFrom());
        setTo(other.getTo());
        _unknown_ = other._unknown_;
    }

    public BDAGEdge copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BDAGEdge copy() {
        var copy = new BDAGEdge();
        copy.assign(this);
        return copy;
    }

    public static void swap(BDAGEdge a, BDAGEdge b) {
        BDAGEdge save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__From extends Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Collections.DAG.BDAGNodeKey> {
        public Log__From(BDAGEdge bean, int varId, Zeze.Builtin.Collections.DAG.BDAGNodeKey value) { super(Zeze.Builtin.Collections.DAG.BDAGNodeKey.class, bean, varId, value); }

        @Override
        public void commit() { ((BDAGEdge)getBelong())._From = value; }
    }

    private static final class Log__To extends Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Collections.DAG.BDAGNodeKey> {
        public Log__To(BDAGEdge bean, int varId, Zeze.Builtin.Collections.DAG.BDAGNodeKey value) { super(Zeze.Builtin.Collections.DAG.BDAGNodeKey.class, bean, varId, value); }

        @Override
        public void commit() { ((BDAGEdge)getBelong())._To = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Collections.DAG.BDAGEdge: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("From=").append(System.lineSeparator());
        getFrom().buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("To=").append(System.lineSeparator());
        getTo().buildString(sb, level + 4);
        sb.append(System.lineSeparator());
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

    @SuppressWarnings("unchecked")
    @Override
    public void followerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _From = ((Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Collections.DAG.BDAGNodeKey>)vlog).value; break;
                case 2: _To = ((Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Collections.DAG.BDAGNodeKey>)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        parents.add("From");
        getFrom().decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
        parents.add("To");
        getTo().decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        parents.add("From");
        getFrom().encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
        parents.add("To");
        getTo().encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "From", "Zeze.Builtin.Collections.DAG.BDAGNodeKey", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "To", "Zeze.Builtin.Collections.DAG.BDAGNodeKey", "", ""));
        return vars;
    }
}
