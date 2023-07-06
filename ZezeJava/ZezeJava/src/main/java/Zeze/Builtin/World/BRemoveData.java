// auto-generated @formatter:off
package Zeze.Builtin.World;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BRemoveData extends Zeze.Transaction.Bean implements BRemoveDataReadOnly {
    public static final long TYPEID = 8336269272864237990L;

    private final Zeze.Transaction.Collections.PList1<Zeze.Builtin.World.ObjectId> _Keys;

    public Zeze.Transaction.Collections.PList1<Zeze.Builtin.World.ObjectId> getKeys() {
        return _Keys;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Zeze.Builtin.World.ObjectId> getKeysReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_Keys);
    }

    @SuppressWarnings("deprecation")
    public BRemoveData() {
        _Keys = new Zeze.Transaction.Collections.PList1<>(Zeze.Builtin.World.ObjectId.class);
        _Keys.variableId(1);
    }

    @Override
    public void reset() {
        _Keys.clear();
    }

    @Override
    public Zeze.Builtin.World.BRemoveData.Data toData() {
        var data = new Zeze.Builtin.World.BRemoveData.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.World.BRemoveData.Data)other);
    }

    public void assign(BRemoveData.Data other) {
        _Keys.clear();
        _Keys.addAll(other._Keys);
    }

    public void assign(BRemoveData other) {
        _Keys.clear();
        _Keys.addAll(other._Keys);
    }

    public BRemoveData copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BRemoveData copy() {
        var copy = new BRemoveData();
        copy.assign(this);
        return copy;
    }

    public static void swap(BRemoveData a, BRemoveData b) {
        BRemoveData save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BRemoveData: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Keys=[");
        if (!_Keys.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Keys) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(System.lineSeparator());
                _item_.buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
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
            var _x_ = _Keys;
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
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.World.ObjectId(), _t_));
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
        _Keys.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Keys.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _Keys) {
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
                case 1: _Keys.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonList(_Keys, Zeze.Builtin.World.ObjectId.class, rs.getString(_parents_name_ + "Keys"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Keys", Zeze.Serialize.Helper.encodeJson(_Keys));
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 8336269272864237990L;

    private java.util.ArrayList<Zeze.Builtin.World.ObjectId> _Keys;

    public java.util.ArrayList<Zeze.Builtin.World.ObjectId> getKeys() {
        return _Keys;
    }

    public void setKeys(java.util.ArrayList<Zeze.Builtin.World.ObjectId> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Keys = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Keys = new java.util.ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    public Data(java.util.ArrayList<Zeze.Builtin.World.ObjectId> _Keys_) {
        if (_Keys_ == null)
            _Keys_ = new java.util.ArrayList<>();
        _Keys = _Keys_;
    }

    @Override
    public void reset() {
        _Keys.clear();
    }

    @Override
    public Zeze.Builtin.World.BRemoveData toBean() {
        var bean = new Zeze.Builtin.World.BRemoveData();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BRemoveData)other);
    }

    public void assign(BRemoveData other) {
        _Keys.clear();
        _Keys.addAll(other._Keys);
    }

    public void assign(BRemoveData.Data other) {
        _Keys.clear();
        _Keys.addAll(other._Keys);
    }

    @Override
    public BRemoveData.Data copy() {
        var copy = new BRemoveData.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BRemoveData.Data a, BRemoveData.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BRemoveData.Data clone() {
        return (BRemoveData.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BRemoveData: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Keys=[");
        if (!_Keys.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Keys) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(System.lineSeparator());
                _item_.buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
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
            var _x_ = _Keys;
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
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.World.ObjectId(), _t_));
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
