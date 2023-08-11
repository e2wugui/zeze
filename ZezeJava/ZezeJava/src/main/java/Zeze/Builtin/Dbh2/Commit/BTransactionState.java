// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Commit;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BTransactionState extends Zeze.Transaction.Bean implements BTransactionStateReadOnly {
    public static final long TYPEID = 7092279656883376454L;

    private int _State;
    private final Zeze.Transaction.Collections.PList1<String> _Buckets;

    @Override
    public int getState() {
        if (!isManaged())
            return _State;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _State;
        var log = (Log__State)txn.getLog(objectId() + 1);
        return log != null ? log.value : _State;
    }

    public void setState(int value) {
        if (!isManaged()) {
            _State = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__State(this, 1, value));
    }

    public Zeze.Transaction.Collections.PList1<String> getBuckets() {
        return _Buckets;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<String> getBucketsReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_Buckets);
    }

    @SuppressWarnings("deprecation")
    public BTransactionState() {
        _Buckets = new Zeze.Transaction.Collections.PList1<>(String.class);
        _Buckets.variableId(2);
    }

    @SuppressWarnings("deprecation")
    public BTransactionState(int _State_) {
        _State = _State_;
        _Buckets = new Zeze.Transaction.Collections.PList1<>(String.class);
        _Buckets.variableId(2);
    }

    @Override
    public void reset() {
        setState(0);
        _Buckets.clear();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.Commit.BTransactionState.Data toData() {
        var data = new Zeze.Builtin.Dbh2.Commit.BTransactionState.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.Commit.BTransactionState.Data)other);
    }

    public void assign(BTransactionState.Data other) {
        setState(other._State);
        _Buckets.clear();
        _Buckets.addAll(other._Buckets);
        _unknown_ = null;
    }

    public void assign(BTransactionState other) {
        setState(other.getState());
        _Buckets.clear();
        _Buckets.addAll(other._Buckets);
        _unknown_ = other._unknown_;
    }

    public BTransactionState copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTransactionState copy() {
        var copy = new BTransactionState();
        copy.assign(this);
        return copy;
    }

    public static void swap(BTransactionState a, BTransactionState b) {
        BTransactionState save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__State extends Zeze.Transaction.Logs.LogInt {
        public Log__State(BTransactionState bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTransactionState)getBelong())._State = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Commit.BTransactionState: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("State=").append(getState()).append(',').append(System.lineSeparator());
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
            int _x_ = getState();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _Buckets;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_) {
                    _o_.WriteString(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setState(_o_.ReadInt(_t_));
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
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Buckets.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Buckets.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getState() < 0)
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
                case 1: _State = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _Buckets.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setState(rs.getInt(_parents_name_ + "State"));
        Zeze.Serialize.Helper.decodeJsonList(_Buckets, String.class, rs.getString(_parents_name_ + "Buckets"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "State", getState());
        st.appendString(_parents_name_ + "Buckets", Zeze.Serialize.Helper.encodeJson(_Buckets));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "State", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Buckets", "list", "", "string"));
        return vars;
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 7092279656883376454L;

    private int _State;
    private java.util.ArrayList<String> _Buckets;

    public int getState() {
        return _State;
    }

    public void setState(int value) {
        _State = value;
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
        _Buckets = new java.util.ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    public Data(int _State_, java.util.ArrayList<String> _Buckets_) {
        _State = _State_;
        if (_Buckets_ == null)
            _Buckets_ = new java.util.ArrayList<>();
        _Buckets = _Buckets_;
    }

    @Override
    public void reset() {
        _State = 0;
        _Buckets.clear();
    }

    @Override
    public Zeze.Builtin.Dbh2.Commit.BTransactionState toBean() {
        var bean = new Zeze.Builtin.Dbh2.Commit.BTransactionState();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BTransactionState)other);
    }

    public void assign(BTransactionState other) {
        _State = other.getState();
        _Buckets.clear();
        _Buckets.addAll(other._Buckets);
    }

    public void assign(BTransactionState.Data other) {
        _State = other._State;
        _Buckets.clear();
        _Buckets.addAll(other._Buckets);
    }

    @Override
    public BTransactionState.Data copy() {
        var copy = new BTransactionState.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BTransactionState.Data a, BTransactionState.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BTransactionState.Data clone() {
        return (BTransactionState.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Commit.BTransactionState: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("State=").append(_State).append(',').append(System.lineSeparator());
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
            int _x_ = _State;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _Buckets;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_) {
                    _o_.WriteString(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _State = _o_.ReadInt(_t_);
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
