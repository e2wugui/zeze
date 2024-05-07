// auto-generated @formatter:off
package Zeze.Builtin.LogService;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BResult extends Zeze.Transaction.Bean implements BResultReadOnly {
    public static final long TYPEID = 5146109133177652644L;

    private final Zeze.Transaction.Collections.PList2<Zeze.Builtin.LogService.BLog> _Logs;
    private boolean _Remain;

    public Zeze.Transaction.Collections.PList2<Zeze.Builtin.LogService.BLog> getLogs() {
        return _Logs;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.LogService.BLog, Zeze.Builtin.LogService.BLogReadOnly> getLogsReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_Logs);
    }

    @Override
    public boolean isRemain() {
        if (!isManaged())
            return _Remain;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Remain;
        var log = (Log__Remain)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Remain;
    }

    public void setRemain(boolean value) {
        if (!isManaged()) {
            _Remain = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Remain(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BResult() {
        _Logs = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.LogService.BLog.class);
        _Logs.variableId(1);
    }

    @SuppressWarnings("deprecation")
    public BResult(boolean _Remain_) {
        _Logs = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.LogService.BLog.class);
        _Logs.variableId(1);
        _Remain = _Remain_;
    }

    @Override
    public void reset() {
        _Logs.clear();
        setRemain(false);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.LogService.BResult.Data toData() {
        var data = new Zeze.Builtin.LogService.BResult.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.LogService.BResult.Data)other);
    }

    public void assign(BResult.Data other) {
        _Logs.clear();
        for (var e : other._Logs) {
            Zeze.Builtin.LogService.BLog data = new Zeze.Builtin.LogService.BLog();
            data.assign(e);
            _Logs.add(data);
        }
        setRemain(other._Remain);
        _unknown_ = null;
    }

    public void assign(BResult other) {
        _Logs.clear();
        for (var e : other._Logs)
            _Logs.add(e.copy());
        setRemain(other.isRemain());
        _unknown_ = other._unknown_;
    }

    public BResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BResult copy() {
        var copy = new BResult();
        copy.assign(this);
        return copy;
    }

    public static void swap(BResult a, BResult b) {
        BResult save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Remain extends Zeze.Transaction.Logs.LogBool {
        public Log__Remain(BResult bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BResult)getBelong())._Remain = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.LogService.BResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Logs=[");
        if (!_Logs.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Logs) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(System.lineSeparator());
                _item_.buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Remain=").append(isRemain()).append(System.lineSeparator());
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
            var _x_ = _Logs;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (var _v_ : _x_) {
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            boolean _x_ = isRemain();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
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
            var _x_ = _Logs;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.LogService.BLog(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setRemain(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Logs.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Logs.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _Logs) {
            if (_v_.negativeCheck())
                return true;
        }
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
                case 1: _Logs.followerApply(vlog); break;
                case 2: _Remain = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonList(_Logs, Zeze.Builtin.LogService.BLog.class, rs.getString(_parents_name_ + "Logs"));
        setRemain(rs.getBoolean(_parents_name_ + "Remain"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Logs", Zeze.Serialize.Helper.encodeJson(_Logs));
        st.appendBoolean(_parents_name_ + "Remain", isRemain());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Logs", "list", "", "Zeze.Builtin.LogService.BLog"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Remain", "bool", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 5146109133177652644L;

    private java.util.ArrayList<Zeze.Builtin.LogService.BLog.Data> _Logs;
    private boolean _Remain;

    public java.util.ArrayList<Zeze.Builtin.LogService.BLog.Data> getLogs() {
        return _Logs;
    }

    public void setLogs(java.util.ArrayList<Zeze.Builtin.LogService.BLog.Data> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Logs = value;
    }

    public boolean isRemain() {
        return _Remain;
    }

    public void setRemain(boolean value) {
        _Remain = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Logs = new java.util.ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    public Data(java.util.ArrayList<Zeze.Builtin.LogService.BLog.Data> _Logs_, boolean _Remain_) {
        if (_Logs_ == null)
            _Logs_ = new java.util.ArrayList<>();
        _Logs = _Logs_;
        _Remain = _Remain_;
    }

    @Override
    public void reset() {
        _Logs.clear();
        _Remain = false;
    }

    @Override
    public Zeze.Builtin.LogService.BResult toBean() {
        var bean = new Zeze.Builtin.LogService.BResult();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BResult)other);
    }

    public void assign(BResult other) {
        _Logs.clear();
        for (var e : other._Logs) {
            Zeze.Builtin.LogService.BLog.Data data = new Zeze.Builtin.LogService.BLog.Data();
            data.assign(e);
            _Logs.add(data);
        }
        _Remain = other.isRemain();
    }

    public void assign(BResult.Data other) {
        _Logs.clear();
        for (var e : other._Logs)
            _Logs.add(e.copy());
        _Remain = other._Remain;
    }

    @Override
    public BResult.Data copy() {
        var copy = new BResult.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BResult.Data a, BResult.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BResult.Data clone() {
        return (BResult.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.LogService.BResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Logs=[");
        if (!_Logs.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Logs) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(System.lineSeparator());
                _item_.buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Remain=").append(_Remain).append(System.lineSeparator());
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
            var _x_ = _Logs;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (int _j_ = 0, _c_ = _x_.size(); _j_ < _c_; _j_++) {
                    var _v_ = _x_.get(_j_);
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            boolean _x_ = _Remain;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _Logs;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.LogService.BLog.Data(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Remain = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
