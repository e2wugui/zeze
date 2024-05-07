// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BBind extends Zeze.Transaction.Bean implements BBindReadOnly {
    public static final long TYPEID = 318036402741860020L;

    public static final int ResultSuccess = 0;
    public static final int ResultFailed = 1;

    private final Zeze.Transaction.Collections.PMap2<Integer, Zeze.Builtin.Provider.BModule> _modules; // moduleId -> BModule
    private final Zeze.Transaction.Collections.PSet1<Long> _linkSids;

    public Zeze.Transaction.Collections.PMap2<Integer, Zeze.Builtin.Provider.BModule> getModules() {
        return _modules;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Builtin.Provider.BModule, Zeze.Builtin.Provider.BModuleReadOnly> getModulesReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_modules);
    }

    public Zeze.Transaction.Collections.PSet1<Long> getLinkSids() {
        return _linkSids;
    }

    @Override
    public Zeze.Transaction.Collections.PSet1ReadOnly<Long> getLinkSidsReadOnly() {
        return new Zeze.Transaction.Collections.PSet1ReadOnly<>(_linkSids);
    }

    @SuppressWarnings("deprecation")
    public BBind() {
        _modules = new Zeze.Transaction.Collections.PMap2<>(Integer.class, Zeze.Builtin.Provider.BModule.class);
        _modules.variableId(1);
        _linkSids = new Zeze.Transaction.Collections.PSet1<>(Long.class);
        _linkSids.variableId(2);
    }

    @Override
    public void reset() {
        _modules.clear();
        _linkSids.clear();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Provider.BBind.Data toData() {
        var data = new Zeze.Builtin.Provider.BBind.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Provider.BBind.Data)other);
    }

    public void assign(BBind.Data other) {
        _modules.clear();
        for (var e : other._modules.entrySet()) {
            Zeze.Builtin.Provider.BModule data = new Zeze.Builtin.Provider.BModule();
            data.assign(e.getValue());
            _modules.put(e.getKey(), data);
        }
        _linkSids.clear();
        _linkSids.addAll(other._linkSids);
        _unknown_ = null;
    }

    public void assign(BBind other) {
        _modules.clear();
        for (var e : other._modules.entrySet())
            _modules.put(e.getKey(), e.getValue().copy());
        _linkSids.clear();
        _linkSids.addAll(other._linkSids);
        _unknown_ = other._unknown_;
    }

    public BBind copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BBind copy() {
        var copy = new BBind();
        copy.assign(this);
        return copy;
    }

    public static void swap(BBind a, BBind b) {
        BBind save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BBind: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("modules={");
        if (!_modules.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _modules.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(System.lineSeparator());
                _kv_.getValue().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("linkSids={");
        if (!_linkSids.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _linkSids) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
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
            var _x_ = _modules;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _linkSids;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
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
    public void decode(IByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _modules;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Provider.BModule(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _linkSids;
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
        _modules.initRootInfo(root, this);
        _linkSids.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _modules.initRootInfoWithRedo(root, this);
        _linkSids.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _modules.values()) {
            if (_v_.negativeCheck())
                return true;
        }
        for (var _v_ : _linkSids) {
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
                case 1: _modules.followerApply(vlog); break;
                case 2: _linkSids.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonMap(this, "modules", _modules, rs.getString(_parents_name_ + "modules"));
        Zeze.Serialize.Helper.decodeJsonSet(_linkSids, Long.class, rs.getString(_parents_name_ + "linkSids"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "modules", Zeze.Serialize.Helper.encodeJson(_modules));
        st.appendString(_parents_name_ + "linkSids", Zeze.Serialize.Helper.encodeJson(_linkSids));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "modules", "map", "int", "Zeze.Builtin.Provider.BModule"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "linkSids", "set", "", "long"));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 318036402741860020L;

    public static final int ResultSuccess = 0;
    public static final int ResultFailed = 1;

    private java.util.HashMap<Integer, Zeze.Builtin.Provider.BModule.Data> _modules; // moduleId -> BModule
    private java.util.HashSet<Long> _linkSids;

    public java.util.HashMap<Integer, Zeze.Builtin.Provider.BModule.Data> getModules() {
        return _modules;
    }

    public void setModules(java.util.HashMap<Integer, Zeze.Builtin.Provider.BModule.Data> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _modules = value;
    }

    public java.util.HashSet<Long> getLinkSids() {
        return _linkSids;
    }

    public void setLinkSids(java.util.HashSet<Long> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _linkSids = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _modules = new java.util.HashMap<>();
        _linkSids = new java.util.HashSet<>();
    }

    @SuppressWarnings("deprecation")
    public Data(java.util.HashMap<Integer, Zeze.Builtin.Provider.BModule.Data> _modules_, java.util.HashSet<Long> _linkSids_) {
        if (_modules_ == null)
            _modules_ = new java.util.HashMap<>();
        _modules = _modules_;
        if (_linkSids_ == null)
            _linkSids_ = new java.util.HashSet<>();
        _linkSids = _linkSids_;
    }

    @Override
    public void reset() {
        _modules.clear();
        _linkSids.clear();
    }

    @Override
    public Zeze.Builtin.Provider.BBind toBean() {
        var bean = new Zeze.Builtin.Provider.BBind();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BBind)other);
    }

    public void assign(BBind other) {
        _modules.clear();
        for (var e : other._modules.entrySet()) {
            Zeze.Builtin.Provider.BModule.Data data = new Zeze.Builtin.Provider.BModule.Data();
            data.assign(e.getValue());
            _modules.put(e.getKey(), data);
        }
        _linkSids.clear();
        _linkSids.addAll(other._linkSids);
    }

    public void assign(BBind.Data other) {
        _modules.clear();
        for (var e : other._modules.entrySet())
            _modules.put(e.getKey(), e.getValue().copy());
        _linkSids.clear();
        _linkSids.addAll(other._linkSids);
    }

    @Override
    public BBind.Data copy() {
        var copy = new BBind.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BBind.Data a, BBind.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BBind.Data clone() {
        return (BBind.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BBind: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("modules={");
        if (!_modules.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _modules.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(System.lineSeparator());
                _kv_.getValue().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("linkSids={");
        if (!_linkSids.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _linkSids) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(System.lineSeparator());
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
            var _x_ = _modules;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _linkSids;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
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
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _modules;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Provider.BModule.Data(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _linkSids;
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
