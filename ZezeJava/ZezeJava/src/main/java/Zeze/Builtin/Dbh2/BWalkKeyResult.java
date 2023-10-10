// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BWalkKeyResult extends Zeze.Transaction.Bean implements BWalkKeyResultReadOnly {
    public static final long TYPEID = 7613011447108499443L;

    private final Zeze.Transaction.Collections.PList1<Zeze.Net.Binary> _Keys;
    private boolean _BucketEnd;
    private boolean _BucketRefuse;

    public Zeze.Transaction.Collections.PList1<Zeze.Net.Binary> getKeys() {
        return _Keys;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Zeze.Net.Binary> getKeysReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_Keys);
    }

    @Override
    public boolean isBucketEnd() {
        if (!isManaged())
            return _BucketEnd;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _BucketEnd;
        var log = (Log__BucketEnd)txn.getLog(objectId() + 2);
        return log != null ? log.value : _BucketEnd;
    }

    public void setBucketEnd(boolean value) {
        if (!isManaged()) {
            _BucketEnd = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__BucketEnd(this, 2, value));
    }

    @Override
    public boolean isBucketRefuse() {
        if (!isManaged())
            return _BucketRefuse;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _BucketRefuse;
        var log = (Log__BucketRefuse)txn.getLog(objectId() + 3);
        return log != null ? log.value : _BucketRefuse;
    }

    public void setBucketRefuse(boolean value) {
        if (!isManaged()) {
            _BucketRefuse = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__BucketRefuse(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BWalkKeyResult() {
        _Keys = new Zeze.Transaction.Collections.PList1<>(Zeze.Net.Binary.class);
        _Keys.variableId(1);
    }

    @SuppressWarnings("deprecation")
    public BWalkKeyResult(boolean _BucketEnd_, boolean _BucketRefuse_) {
        _Keys = new Zeze.Transaction.Collections.PList1<>(Zeze.Net.Binary.class);
        _Keys.variableId(1);
        _BucketEnd = _BucketEnd_;
        _BucketRefuse = _BucketRefuse_;
    }

    @Override
    public void reset() {
        _Keys.clear();
        setBucketEnd(false);
        setBucketRefuse(false);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.BWalkKeyResult.Data toData() {
        var data = new Zeze.Builtin.Dbh2.BWalkKeyResult.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.BWalkKeyResult.Data)other);
    }

    public void assign(BWalkKeyResult.Data other) {
        _Keys.clear();
        _Keys.addAll(other._Keys);
        setBucketEnd(other._BucketEnd);
        setBucketRefuse(other._BucketRefuse);
        _unknown_ = null;
    }

    public void assign(BWalkKeyResult other) {
        _Keys.clear();
        _Keys.addAll(other._Keys);
        setBucketEnd(other.isBucketEnd());
        setBucketRefuse(other.isBucketRefuse());
        _unknown_ = other._unknown_;
    }

    public BWalkKeyResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BWalkKeyResult copy() {
        var copy = new BWalkKeyResult();
        copy.assign(this);
        return copy;
    }

    public static void swap(BWalkKeyResult a, BWalkKeyResult b) {
        BWalkKeyResult save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__BucketEnd extends Zeze.Transaction.Logs.LogBool {
        public Log__BucketEnd(BWalkKeyResult bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BWalkKeyResult)getBelong())._BucketEnd = value; }
    }

    private static final class Log__BucketRefuse extends Zeze.Transaction.Logs.LogBool {
        public Log__BucketRefuse(BWalkKeyResult bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BWalkKeyResult)getBelong())._BucketRefuse = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BWalkKeyResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Keys=[");
        if (!_Keys.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Keys) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("BucketEnd=").append(isBucketEnd()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("BucketRefuse=").append(isBucketRefuse()).append(System.lineSeparator());
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
            var _x_ = _Keys;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_) {
                    _o_.WriteBinary(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            boolean _x_ = isBucketEnd();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            boolean _x_ = isBucketRefuse();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
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
            var _x_ = _Keys;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBinary(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setBucketEnd(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setBucketRefuse(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Keys.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Keys.initRootInfoWithRedo(root, this);
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
                case 1: _Keys.followerApply(vlog); break;
                case 2: _BucketEnd = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
                case 3: _BucketRefuse = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonList(_Keys, Zeze.Net.Binary.class, rs.getString(_parents_name_ + "Keys"));
        setBucketEnd(rs.getBoolean(_parents_name_ + "BucketEnd"));
        setBucketRefuse(rs.getBoolean(_parents_name_ + "BucketRefuse"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Keys", Zeze.Serialize.Helper.encodeJson(_Keys));
        st.appendBoolean(_parents_name_ + "BucketEnd", isBucketEnd());
        st.appendBoolean(_parents_name_ + "BucketRefuse", isBucketRefuse());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Keys", "list", "", "binary"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "BucketEnd", "bool", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "BucketRefuse", "bool", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 7613011447108499443L;

    private java.util.ArrayList<Zeze.Net.Binary> _Keys;
    private boolean _BucketEnd;
    private boolean _BucketRefuse;

    public java.util.ArrayList<Zeze.Net.Binary> getKeys() {
        return _Keys;
    }

    public void setKeys(java.util.ArrayList<Zeze.Net.Binary> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Keys = value;
    }

    public boolean isBucketEnd() {
        return _BucketEnd;
    }

    public void setBucketEnd(boolean value) {
        _BucketEnd = value;
    }

    public boolean isBucketRefuse() {
        return _BucketRefuse;
    }

    public void setBucketRefuse(boolean value) {
        _BucketRefuse = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Keys = new java.util.ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    public Data(java.util.ArrayList<Zeze.Net.Binary> _Keys_, boolean _BucketEnd_, boolean _BucketRefuse_) {
        if (_Keys_ == null)
            _Keys_ = new java.util.ArrayList<>();
        _Keys = _Keys_;
        _BucketEnd = _BucketEnd_;
        _BucketRefuse = _BucketRefuse_;
    }

    @Override
    public void reset() {
        _Keys.clear();
        _BucketEnd = false;
        _BucketRefuse = false;
    }

    @Override
    public Zeze.Builtin.Dbh2.BWalkKeyResult toBean() {
        var bean = new Zeze.Builtin.Dbh2.BWalkKeyResult();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BWalkKeyResult)other);
    }

    public void assign(BWalkKeyResult other) {
        _Keys.clear();
        _Keys.addAll(other._Keys);
        _BucketEnd = other.isBucketEnd();
        _BucketRefuse = other.isBucketRefuse();
    }

    public void assign(BWalkKeyResult.Data other) {
        _Keys.clear();
        _Keys.addAll(other._Keys);
        _BucketEnd = other._BucketEnd;
        _BucketRefuse = other._BucketRefuse;
    }

    @Override
    public BWalkKeyResult.Data copy() {
        var copy = new BWalkKeyResult.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BWalkKeyResult.Data a, BWalkKeyResult.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BWalkKeyResult.Data clone() {
        return (BWalkKeyResult.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BWalkKeyResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Keys=[");
        if (!_Keys.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Keys) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("BucketEnd=").append(_BucketEnd).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("BucketRefuse=").append(_BucketRefuse).append(System.lineSeparator());
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
            var _x_ = _Keys;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (int _j_ = 0, _c_ = _x_.size(); _j_ < _c_; _j_++) {
                    var _v_ = _x_.get(_j_);
                    _o_.WriteBinary(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            boolean _x_ = _BucketEnd;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            boolean _x_ = _BucketRefuse;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _Keys;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBinary(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _BucketEnd = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _BucketRefuse = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
