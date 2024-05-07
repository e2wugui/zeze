// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BListServiceResult extends Zeze.Transaction.Bean implements BListServiceResultReadOnly {
    public static final long TYPEID = -2243510208432437687L;

    private final Zeze.Transaction.Collections.PList2<Zeze.Builtin.Zoker.BService> _Services;

    public Zeze.Transaction.Collections.PList2<Zeze.Builtin.Zoker.BService> getServices() {
        return _Services;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Zoker.BService, Zeze.Builtin.Zoker.BServiceReadOnly> getServicesReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_Services);
    }

    @SuppressWarnings("deprecation")
    public BListServiceResult() {
        _Services = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.Zoker.BService.class);
        _Services.variableId(1);
    }

    @Override
    public void reset() {
        _Services.clear();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Zoker.BListServiceResult.Data toData() {
        var data = new Zeze.Builtin.Zoker.BListServiceResult.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Zoker.BListServiceResult.Data)other);
    }

    public void assign(BListServiceResult.Data other) {
        _Services.clear();
        for (var e : other._Services) {
            Zeze.Builtin.Zoker.BService data = new Zeze.Builtin.Zoker.BService();
            data.assign(e);
            _Services.add(data);
        }
        _unknown_ = null;
    }

    public void assign(BListServiceResult other) {
        _Services.clear();
        for (var e : other._Services)
            _Services.add(e.copy());
        _unknown_ = other._unknown_;
    }

    public BListServiceResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BListServiceResult copy() {
        var copy = new BListServiceResult();
        copy.assign(this);
        return copy;
    }

    public static void swap(BListServiceResult a, BListServiceResult b) {
        BListServiceResult save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Zoker.BListServiceResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Services=[");
        if (!_Services.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Services) {
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
            var _x_ = _Services;
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
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _Services;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.Zoker.BService(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Services.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Services.initRootInfoWithRedo(root, this);
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
                case 1: _Services.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonList(_Services, Zeze.Builtin.Zoker.BService.class, rs.getString(_parents_name_ + "Services"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Services", Zeze.Serialize.Helper.encodeJson(_Services));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Services", "list", "", "Zeze.Builtin.Zoker.BService"));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -2243510208432437687L;

    private java.util.ArrayList<Zeze.Builtin.Zoker.BService.Data> _Services;

    public java.util.ArrayList<Zeze.Builtin.Zoker.BService.Data> getServices() {
        return _Services;
    }

    public void setServices(java.util.ArrayList<Zeze.Builtin.Zoker.BService.Data> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Services = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Services = new java.util.ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    public Data(java.util.ArrayList<Zeze.Builtin.Zoker.BService.Data> _Services_) {
        if (_Services_ == null)
            _Services_ = new java.util.ArrayList<>();
        _Services = _Services_;
    }

    @Override
    public void reset() {
        _Services.clear();
    }

    @Override
    public Zeze.Builtin.Zoker.BListServiceResult toBean() {
        var bean = new Zeze.Builtin.Zoker.BListServiceResult();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BListServiceResult)other);
    }

    public void assign(BListServiceResult other) {
        _Services.clear();
        for (var e : other._Services) {
            Zeze.Builtin.Zoker.BService.Data data = new Zeze.Builtin.Zoker.BService.Data();
            data.assign(e);
            _Services.add(data);
        }
    }

    public void assign(BListServiceResult.Data other) {
        _Services.clear();
        for (var e : other._Services)
            _Services.add(e.copy());
    }

    @Override
    public BListServiceResult.Data copy() {
        var copy = new BListServiceResult.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BListServiceResult.Data a, BListServiceResult.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BListServiceResult.Data clone() {
        return (BListServiceResult.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Zoker.BListServiceResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Services=[");
        if (!_Services.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Services) {
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
            var _x_ = _Services;
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
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _Services;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.Zoker.BService.Data(), _t_));
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
