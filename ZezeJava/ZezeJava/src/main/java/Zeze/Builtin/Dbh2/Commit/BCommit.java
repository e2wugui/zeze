// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Commit;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BCommit extends Zeze.Transaction.Bean implements BCommitReadOnly {
    public static final long TYPEID = 3148866502298196978L;

    private Zeze.Net.Binary _Tid;
    private final Zeze.Transaction.Collections.PList1<String> _Buckets;

    @Override
    public Zeze.Net.Binary getTid() {
        if (!isManaged())
            return _Tid;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Tid;
        var log = (Log__Tid)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Tid;
    }

    public void setTid(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Tid = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Tid(this, 1, value));
    }

    public Zeze.Transaction.Collections.PList1<String> getBuckets() {
        return _Buckets;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<String> getBucketsReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_Buckets);
    }

    @SuppressWarnings("deprecation")
    public BCommit() {
        _Tid = Zeze.Net.Binary.Empty;
        _Buckets = new Zeze.Transaction.Collections.PList1<>(String.class);
        _Buckets.variableId(2);
    }

    @SuppressWarnings("deprecation")
    public BCommit(Zeze.Net.Binary _Tid_) {
        if (_Tid_ == null)
            throw new IllegalArgumentException();
        _Tid = _Tid_;
        _Buckets = new Zeze.Transaction.Collections.PList1<>(String.class);
        _Buckets.variableId(2);
    }

    @Override
    public Zeze.Builtin.Dbh2.Commit.BCommit.Data toData() {
        var data = new Zeze.Builtin.Dbh2.Commit.BCommit.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.Commit.BCommit.Data)other);
    }

    public void assign(BCommit.Data other) {
        setTid(other.getTid());
        _Buckets.clear();
        _Buckets.addAll(other.getBuckets());
    }

    public void assign(BCommit other) {
        setTid(other.getTid());
        _Buckets.clear();
        _Buckets.addAll(other.getBuckets());
    }

    public BCommit copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCommit copy() {
        var copy = new BCommit();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCommit a, BCommit b) {
        BCommit save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Tid extends Zeze.Transaction.Logs.LogBinary {
        public Log__Tid(BCommit bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCommit)getBelong())._Tid = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Commit.BCommit: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Tid=").append(getTid()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Buckets=[");
        if (!_Buckets.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Buckets) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(System.lineSeparator());
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

    @Override
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            var _x_ = getTid();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = _Buckets;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_)
                    _o_.WriteString(_v_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setTid(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Buckets;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadString(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Buckets.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Buckets.initRootInfoWithRedo(root, this);
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
                case 1: _Tid = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 2: _Buckets.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setTid(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "Tid")));
        if (getTid() == null)
            setTid(Zeze.Net.Binary.Empty);
        Zeze.Serialize.Helper.decodeJsonList(getBuckets(), String.class, rs.getString(_parents_name_ + "Buckets"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendBinary(_parents_name_ + "Tid", getTid());
        st.appendString(_parents_name_ + "Buckets", Zeze.Serialize.Helper.encodeJson(getBuckets()));
    }

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 3148866502298196978L;

    private Zeze.Net.Binary _Tid;
    private java.util.ArrayList<String> _Buckets;

    public Zeze.Net.Binary getTid() {
        return _Tid;
    }

    public void setTid(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Tid = value;
    }

    public java.util.ArrayList<String> getBuckets() {
        return _Buckets;
    }

    public void setBuckets(java.util.ArrayList<String> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Buckets = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Tid = Zeze.Net.Binary.Empty;
        _Buckets = new java.util.ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Net.Binary _Tid_) {
        if (_Tid_ == null)
            throw new IllegalArgumentException();
        _Tid = _Tid_;
        _Buckets = new java.util.ArrayList<>();
    }

    @Override
    public Zeze.Builtin.Dbh2.Commit.BCommit toBean() {
        var bean = new Zeze.Builtin.Dbh2.Commit.BCommit();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BCommit)other);
    }

    public void assign(BCommit other) {
        setTid(other.getTid());
        _Buckets.clear();
        _Buckets.addAll(other.getBuckets());
    }

    public void assign(BCommit.Data other) {
        setTid(other.getTid());
        _Buckets.clear();
        _Buckets.addAll(other.getBuckets());
    }

    @Override
    public BCommit.Data copy() {
        var copy = new BCommit.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCommit.Data a, BCommit.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Commit.BCommit: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Tid=").append(getTid()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Buckets=[");
        if (!_Buckets.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Buckets) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(System.lineSeparator());
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

    @Override
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            var _x_ = getTid();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = _Buckets;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_)
                    _o_.WriteString(_v_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setTid(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Buckets;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadString(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}