// auto-generated @formatter:off
package Zeze.Builtin.Collections.LinkedMap;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BLinkedMap extends Zeze.Transaction.Bean implements BLinkedMapReadOnly {
    public static final long TYPEID = -8443895985300072767L;

    private long _HeadNodeId;
    private long _TailNodeId;
    private long _Count;
    private long _LastNodeId; // 最近分配过的NodeId, 用于下次分配

    @Override
    public long getHeadNodeId() {
        if (!isManaged())
            return _HeadNodeId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _HeadNodeId;
        var log = (Log__HeadNodeId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _HeadNodeId;
    }

    public void setHeadNodeId(long value) {
        if (!isManaged()) {
            _HeadNodeId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__HeadNodeId(this, 1, value));
    }

    @Override
    public long getTailNodeId() {
        if (!isManaged())
            return _TailNodeId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TailNodeId;
        var log = (Log__TailNodeId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _TailNodeId;
    }

    public void setTailNodeId(long value) {
        if (!isManaged()) {
            _TailNodeId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TailNodeId(this, 2, value));
    }

    @Override
    public long getCount() {
        if (!isManaged())
            return _Count;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Count;
        var log = (Log__Count)txn.getLog(objectId() + 3);
        return log != null ? log.value : _Count;
    }

    public void setCount(long value) {
        if (!isManaged()) {
            _Count = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Count(this, 3, value));
    }

    @Override
    public long getLastNodeId() {
        if (!isManaged())
            return _LastNodeId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _LastNodeId;
        var log = (Log__LastNodeId)txn.getLog(objectId() + 4);
        return log != null ? log.value : _LastNodeId;
    }

    public void setLastNodeId(long value) {
        if (!isManaged()) {
            _LastNodeId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__LastNodeId(this, 4, value));
    }

    @SuppressWarnings("deprecation")
    public BLinkedMap() {
    }

    @SuppressWarnings("deprecation")
    public BLinkedMap(long _HeadNodeId_, long _TailNodeId_, long _Count_, long _LastNodeId_) {
        _HeadNodeId = _HeadNodeId_;
        _TailNodeId = _TailNodeId_;
        _Count = _Count_;
        _LastNodeId = _LastNodeId_;
    }

    @Override
    public void reset() {
        setHeadNodeId(0);
        setTailNodeId(0);
        setCount(0);
        setLastNodeId(0);
        _unknown_ = null;
    }

    public void assign(BLinkedMap other) {
        setHeadNodeId(other.getHeadNodeId());
        setTailNodeId(other.getTailNodeId());
        setCount(other.getCount());
        setLastNodeId(other.getLastNodeId());
        _unknown_ = other._unknown_;
    }

    public BLinkedMap copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLinkedMap copy() {
        var copy = new BLinkedMap();
        copy.assign(this);
        return copy;
    }

    public static void swap(BLinkedMap a, BLinkedMap b) {
        BLinkedMap save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__HeadNodeId extends Zeze.Transaction.Logs.LogLong {
        public Log__HeadNodeId(BLinkedMap bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLinkedMap)getBelong())._HeadNodeId = value; }
    }

    private static final class Log__TailNodeId extends Zeze.Transaction.Logs.LogLong {
        public Log__TailNodeId(BLinkedMap bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLinkedMap)getBelong())._TailNodeId = value; }
    }

    private static final class Log__Count extends Zeze.Transaction.Logs.LogLong {
        public Log__Count(BLinkedMap bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLinkedMap)getBelong())._Count = value; }
    }

    private static final class Log__LastNodeId extends Zeze.Transaction.Logs.LogLong {
        public Log__LastNodeId(BLinkedMap bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLinkedMap)getBelong())._LastNodeId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Collections.LinkedMap.BLinkedMap: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("HeadNodeId=").append(getHeadNodeId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TailNodeId=").append(getTailNodeId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Count=").append(getCount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LastNodeId=").append(getLastNodeId()).append(System.lineSeparator());
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
            long _x_ = getHeadNodeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getTailNodeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getLastNodeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
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
            setHeadNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setTailNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setCount(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setLastNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BLinkedMap))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLinkedMap)_o_;
        if (getHeadNodeId() != _b_.getHeadNodeId())
            return false;
        if (getTailNodeId() != _b_.getTailNodeId())
            return false;
        if (getCount() != _b_.getCount())
            return false;
        if (getLastNodeId() != _b_.getLastNodeId())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getHeadNodeId() < 0)
            return true;
        if (getTailNodeId() < 0)
            return true;
        if (getCount() < 0)
            return true;
        if (getLastNodeId() < 0)
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
                case 1: _HeadNodeId = vlog.longValue(); break;
                case 2: _TailNodeId = vlog.longValue(); break;
                case 3: _Count = vlog.longValue(); break;
                case 4: _LastNodeId = vlog.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setHeadNodeId(rs.getLong(_parents_name_ + "HeadNodeId"));
        setTailNodeId(rs.getLong(_parents_name_ + "TailNodeId"));
        setCount(rs.getLong(_parents_name_ + "Count"));
        setLastNodeId(rs.getLong(_parents_name_ + "LastNodeId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "HeadNodeId", getHeadNodeId());
        st.appendLong(_parents_name_ + "TailNodeId", getTailNodeId());
        st.appendLong(_parents_name_ + "Count", getCount());
        st.appendLong(_parents_name_ + "LastNodeId", getLastNodeId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "HeadNodeId", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "TailNodeId", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Count", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "LastNodeId", "long", "", ""));
        return vars;
    }
}
