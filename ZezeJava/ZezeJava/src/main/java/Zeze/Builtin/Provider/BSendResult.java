// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BSendResult extends Zeze.Transaction.Bean implements BSendResultReadOnly {
    public static final long TYPEID = -7186434891670297524L;

    private final Zeze.Transaction.Collections.PList1<Long> _ErrorLinkSids;

    public Zeze.Transaction.Collections.PList1<Long> getErrorLinkSids() {
        return _ErrorLinkSids;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getErrorLinkSidsReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_ErrorLinkSids);
    }

    @SuppressWarnings("deprecation")
    public BSendResult() {
        _ErrorLinkSids = new Zeze.Transaction.Collections.PList1<>(Long.class);
        _ErrorLinkSids.variableId(1);
    }

    @Override
    public void reset() {
        _ErrorLinkSids.clear();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Provider.BSendResult.Data toData() {
        var data = new Zeze.Builtin.Provider.BSendResult.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Provider.BSendResult.Data)other);
    }

    public void assign(BSendResult.Data other) {
        _ErrorLinkSids.clear();
        _ErrorLinkSids.addAll(other._ErrorLinkSids);
        _unknown_ = null;
    }

    public void assign(BSendResult other) {
        _ErrorLinkSids.clear();
        _ErrorLinkSids.addAll(other._ErrorLinkSids);
        _unknown_ = other._unknown_;
    }

    public BSendResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSendResult copy() {
        var copy = new BSendResult();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSendResult a, BSendResult b) {
        BSendResult save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BSendResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ErrorLinkSids=[");
        if (!_ErrorLinkSids.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _ErrorLinkSids) {
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
            var _x_ = _ErrorLinkSids;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_) {
                    _o_.WriteLong(_v_);
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
            var _x_ = _ErrorLinkSids;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _ErrorLinkSids.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _ErrorLinkSids.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _ErrorLinkSids) {
            if (_v_ < 0)
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
                case 1: _ErrorLinkSids.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonList(_ErrorLinkSids, Long.class, rs.getString(_parents_name_ + "ErrorLinkSids"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "ErrorLinkSids", Zeze.Serialize.Helper.encodeJson(_ErrorLinkSids));
    }

    @Override
    public java.util.List<Zeze.Transaction.Bean.Variable> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Transaction.Bean.Variable(1, "ErrorLinkSids", "list", "", "long"));
        return vars;
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BSendResult
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -7186434891670297524L;

    private java.util.ArrayList<Long> _ErrorLinkSids;

    public java.util.ArrayList<Long> getErrorLinkSids() {
        return _ErrorLinkSids;
    }

    public void setErrorLinkSids(java.util.ArrayList<Long> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _ErrorLinkSids = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _ErrorLinkSids = new java.util.ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    public Data(java.util.ArrayList<Long> _ErrorLinkSids_) {
        if (_ErrorLinkSids_ == null)
            _ErrorLinkSids_ = new java.util.ArrayList<>();
        _ErrorLinkSids = _ErrorLinkSids_;
    }

    @Override
    public void reset() {
        _ErrorLinkSids.clear();
    }

    @Override
    public Zeze.Builtin.Provider.BSendResult toBean() {
        var bean = new Zeze.Builtin.Provider.BSendResult();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BSendResult)other);
    }

    public void assign(BSendResult other) {
        _ErrorLinkSids.clear();
        _ErrorLinkSids.addAll(other._ErrorLinkSids);
    }

    public void assign(BSendResult.Data other) {
        _ErrorLinkSids.clear();
        _ErrorLinkSids.addAll(other._ErrorLinkSids);
    }

    @Override
    public BSendResult.Data copy() {
        var copy = new BSendResult.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSendResult.Data a, BSendResult.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BSendResult.Data clone() {
        return (BSendResult.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BSendResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ErrorLinkSids=[");
        if (!_ErrorLinkSids.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _ErrorLinkSids) {
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
            var _x_ = _ErrorLinkSids;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_) {
                    _o_.WriteLong(_v_);
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
            var _x_ = _ErrorLinkSids;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
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
