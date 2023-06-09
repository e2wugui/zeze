// auto-generated @formatter:off
package Zeze.Builtin.Threading;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BQueryLockInfo extends Zeze.Transaction.Bean implements BQueryLockInfoReadOnly {
    public static final long TYPEID = -5570759109568176767L;

    private final Zeze.Transaction.Collections.PList1<String> _LockNames; // 请求包含所有当前线程拥有的锁，结果是剩下的不存在的锁。

    public Zeze.Transaction.Collections.PList1<String> getLockNames() {
        return _LockNames;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<String> getLockNamesReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_LockNames);
    }

    @SuppressWarnings("deprecation")
    public BQueryLockInfo() {
        _LockNames = new Zeze.Transaction.Collections.PList1<>(String.class);
        _LockNames.variableId(1);
    }

    @Override
    public Zeze.Builtin.Threading.BQueryLockInfo.Data toData() {
        var data = new Zeze.Builtin.Threading.BQueryLockInfo.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Threading.BQueryLockInfo.Data)other);
    }

    public void assign(BQueryLockInfo.Data other) {
        _LockNames.clear();
        _LockNames.addAll(other._LockNames);
    }

    public void assign(BQueryLockInfo other) {
        _LockNames.clear();
        _LockNames.addAll(other._LockNames);
    }

    public BQueryLockInfo copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BQueryLockInfo copy() {
        var copy = new BQueryLockInfo();
        copy.assign(this);
        return copy;
    }

    public static void swap(BQueryLockInfo a, BQueryLockInfo b) {
        BQueryLockInfo save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Threading.BQueryLockInfo: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("LockNames=[");
        if (!_LockNames.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _LockNames) {
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
            var _x_ = _LockNames;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
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
            var _x_ = _LockNames;
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
        _LockNames.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _LockNames.initRootInfoWithRedo(root, this);
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
                case 1: _LockNames.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonList(_LockNames, String.class, rs.getString(_parents_name_ + "LockNames"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "LockNames", Zeze.Serialize.Helper.encodeJson(_LockNames));
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -5570759109568176767L;

    private java.util.ArrayList<String> _LockNames; // 请求包含所有当前线程拥有的锁，结果是剩下的不存在的锁。

    public java.util.ArrayList<String> getLockNames() {
        return _LockNames;
    }

    public void setLockNames(java.util.ArrayList<String> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _LockNames = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _LockNames = new java.util.ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    public Data(java.util.ArrayList<String> _LockNames_) {
        if (_LockNames_ == null)
            _LockNames_ = new java.util.ArrayList<>();
        _LockNames = _LockNames_;
    }

    @Override
    public Zeze.Builtin.Threading.BQueryLockInfo toBean() {
        var bean = new Zeze.Builtin.Threading.BQueryLockInfo();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BQueryLockInfo)other);
    }

    public void assign(BQueryLockInfo other) {
        _LockNames.clear();
        _LockNames.addAll(other._LockNames);
    }

    public void assign(BQueryLockInfo.Data other) {
        _LockNames.clear();
        _LockNames.addAll(other._LockNames);
    }

    @Override
    public BQueryLockInfo.Data copy() {
        var copy = new BQueryLockInfo.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BQueryLockInfo.Data a, BQueryLockInfo.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BQueryLockInfo.Data clone() {
        return (BQueryLockInfo.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Threading.BQueryLockInfo: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("LockNames=[");
        if (!_LockNames.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _LockNames) {
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
            var _x_ = _LockNames;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
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
            var _x_ = _LockNames;
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
