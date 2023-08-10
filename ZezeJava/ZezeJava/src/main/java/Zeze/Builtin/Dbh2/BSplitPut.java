// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BSplitPut extends Zeze.Transaction.Bean implements BSplitPutReadOnly {
    public static final long TYPEID = 5420980520394401381L;

    private boolean _fromTransaction;
    private final Zeze.Transaction.Collections.PMap1<Zeze.Net.Binary, Zeze.Net.Binary> _Puts; // 包含delete，用Binary.Empty表示。

    @Override
    public boolean isFromTransaction() {
        if (!isManaged())
            return _fromTransaction;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _fromTransaction;
        var log = (Log__fromTransaction)txn.getLog(objectId() + 1);
        return log != null ? log.value : _fromTransaction;
    }

    public void setFromTransaction(boolean value) {
        if (!isManaged()) {
            _fromTransaction = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__fromTransaction(this, 1, value));
    }

    public Zeze.Transaction.Collections.PMap1<Zeze.Net.Binary, Zeze.Net.Binary> getPuts() {
        return _Puts;
    }

    @Override
    public Zeze.Transaction.Collections.PMap1ReadOnly<Zeze.Net.Binary, Zeze.Net.Binary> getPutsReadOnly() {
        return new Zeze.Transaction.Collections.PMap1ReadOnly<>(_Puts);
    }

    @SuppressWarnings("deprecation")
    public BSplitPut() {
        _Puts = new Zeze.Transaction.Collections.PMap1<>(Zeze.Net.Binary.class, Zeze.Net.Binary.class);
        _Puts.variableId(2);
    }

    @SuppressWarnings("deprecation")
    public BSplitPut(boolean _fromTransaction_) {
        _fromTransaction = _fromTransaction_;
        _Puts = new Zeze.Transaction.Collections.PMap1<>(Zeze.Net.Binary.class, Zeze.Net.Binary.class);
        _Puts.variableId(2);
    }

    @Override
    public void reset() {
        setFromTransaction(false);
        _Puts.clear();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.BSplitPut.Data toData() {
        var data = new Zeze.Builtin.Dbh2.BSplitPut.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.BSplitPut.Data)other);
    }

    public void assign(BSplitPut.Data other) {
        setFromTransaction(other._fromTransaction);
        _Puts.clear();
        _Puts.putAll(other._Puts);
        _unknown_ = null;
    }

    public void assign(BSplitPut other) {
        setFromTransaction(other.isFromTransaction());
        _Puts.clear();
        _Puts.putAll(other._Puts);
        _unknown_ = other._unknown_;
    }

    public BSplitPut copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSplitPut copy() {
        var copy = new BSplitPut();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSplitPut a, BSplitPut b) {
        BSplitPut save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__fromTransaction extends Zeze.Transaction.Logs.LogBool {
        public Log__fromTransaction(BSplitPut bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSplitPut)getBelong())._fromTransaction = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BSplitPut: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("fromTransaction=").append(isFromTransaction()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Puts={");
        if (!_Puts.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Puts.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(_kv_.getValue()).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(System.lineSeparator());
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
            boolean _x_ = isFromTransaction();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            var _x_ = _Puts;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BYTES);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteBinary(_e_.getKey());
                    _o_.WriteBinary(_e_.getValue());
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
            setFromTransaction(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Puts;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadBinary(_s_);
                    var _v_ = _o_.ReadBinary(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Puts.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Puts.initRootInfoWithRedo(root, this);
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
                case 1: _fromTransaction = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
                case 2: _Puts.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setFromTransaction(rs.getBoolean(_parents_name_ + "fromTransaction"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "Puts", _Puts, rs.getString(_parents_name_ + "Puts"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendBoolean(_parents_name_ + "fromTransaction", isFromTransaction());
        st.appendString(_parents_name_ + "Puts", Zeze.Serialize.Helper.encodeJson(_Puts));
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BSplitPut
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 5420980520394401381L;

    private boolean _fromTransaction;
    private java.util.HashMap<Zeze.Net.Binary, Zeze.Net.Binary> _Puts; // 包含delete，用Binary.Empty表示。

    public boolean isFromTransaction() {
        return _fromTransaction;
    }

    public void setFromTransaction(boolean value) {
        _fromTransaction = value;
    }

    public java.util.HashMap<Zeze.Net.Binary, Zeze.Net.Binary> getPuts() {
        return _Puts;
    }

    public void setPuts(java.util.HashMap<Zeze.Net.Binary, Zeze.Net.Binary> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Puts = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Puts = new java.util.HashMap<>();
    }

    @SuppressWarnings("deprecation")
    public Data(boolean _fromTransaction_, java.util.HashMap<Zeze.Net.Binary, Zeze.Net.Binary> _Puts_) {
        _fromTransaction = _fromTransaction_;
        if (_Puts_ == null)
            _Puts_ = new java.util.HashMap<>();
        _Puts = _Puts_;
    }

    @Override
    public void reset() {
        _fromTransaction = false;
        _Puts.clear();
    }

    @Override
    public Zeze.Builtin.Dbh2.BSplitPut toBean() {
        var bean = new Zeze.Builtin.Dbh2.BSplitPut();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BSplitPut)other);
    }

    public void assign(BSplitPut other) {
        _fromTransaction = other.isFromTransaction();
        _Puts.clear();
        _Puts.putAll(other._Puts);
    }

    public void assign(BSplitPut.Data other) {
        _fromTransaction = other._fromTransaction;
        _Puts.clear();
        _Puts.putAll(other._Puts);
    }

    @Override
    public BSplitPut.Data copy() {
        var copy = new BSplitPut.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSplitPut.Data a, BSplitPut.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BSplitPut.Data clone() {
        return (BSplitPut.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BSplitPut: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("fromTransaction=").append(_fromTransaction).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Puts={");
        if (!_Puts.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Puts.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(_kv_.getValue()).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(System.lineSeparator());
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
            boolean _x_ = _fromTransaction;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            var _x_ = _Puts;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BYTES);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteBinary(_e_.getKey());
                    _o_.WriteBinary(_e_.getValue());
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
            _fromTransaction = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Puts;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadBinary(_s_);
                    var _v_ = _o_.ReadBinary(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
