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
        var _d_ = new Zeze.Builtin.Provider.BBind.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Provider.BBind.Data)_o_);
    }

    public void assign(BBind.Data _o_) {
        _modules.clear();
        for (var _e_ : _o_._modules.entrySet()) {
            var _v_ = new Zeze.Builtin.Provider.BModule();
            _v_.assign(_e_.getValue());
            _modules.put(_e_.getKey(), _v_);
        }
        _linkSids.clear();
        _linkSids.addAll(_o_._linkSids);
        _unknown_ = null;
    }

    public void assign(BBind _o_) {
        _modules.clear();
        for (var _e_ : _o_._modules.entrySet())
            _modules.put(_e_.getKey(), _e_.getValue().copy());
        _linkSids.assign(_o_._linkSids);
        _unknown_ = _o_._unknown_;
    }

    public BBind copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BBind copy() {
        var _c_ = new BBind();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BBind _a_, BBind _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("Zeze.Builtin.Provider.BBind: {\n");
        _s_.append(_i1_).append("modules={");
        if (!_modules.isEmpty()) {
            _s_.append('\n');
            for (var _e_ : _modules.entrySet()) {
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("linkSids={");
        if (!_linkSids.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _linkSids) {
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("}\n");
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
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
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BBind))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BBind)_o_;
        if (!_modules.equals(_b_._modules))
            return false;
        if (!_linkSids.equals(_b_._linkSids))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _modules.initRootInfo(_r_, this);
        _linkSids.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _modules.initRootInfoWithRedo(_r_, this);
        _linkSids.initRootInfoWithRedo(_r_, this);
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
    public void followerApply(Zeze.Transaction.Log _l_) {
        var _vs_ = ((Zeze.Transaction.Collections.LogBean)_l_).getVariables();
        if (_vs_ == null)
            return;
        for (var _i_ = _vs_.iterator(); _i_.moveToNext(); ) {
            var _v_ = _i_.value();
            switch (_v_.getVariableId()) {
                case 1: _modules.followerApply(_v_); break;
                case 2: _linkSids.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        Zeze.Serialize.Helper.decodeJsonMap(this, "modules", _modules, _r_.getString(_pn_ + "modules"));
        Zeze.Serialize.Helper.decodeJsonSet(_linkSids, Long.class, _r_.getString(_pn_ + "linkSids"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "modules", Zeze.Serialize.Helper.encodeJson(_modules));
        _s_.appendString(_pn_ + "linkSids", Zeze.Serialize.Helper.encodeJson(_linkSids));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "modules", "map", "int", "Zeze.Builtin.Provider.BModule"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "linkSids", "set", "", "long"));
        return _v_;
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

    public void setModules(java.util.HashMap<Integer, Zeze.Builtin.Provider.BModule.Data> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _modules = _v_;
    }

    public java.util.HashSet<Long> getLinkSids() {
        return _linkSids;
    }

    public void setLinkSids(java.util.HashSet<Long> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _linkSids = _v_;
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
        var _b_ = new Zeze.Builtin.Provider.BBind();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BBind)_o_);
    }

    public void assign(BBind _o_) {
        _modules.clear();
        for (var _e_ : _o_._modules.entrySet()) {
            var _v_ = new Zeze.Builtin.Provider.BModule.Data();
            _v_.assign(_e_.getValue());
            _modules.put(_e_.getKey(), _v_);
        }
        _linkSids.clear();
        _linkSids.addAll(_o_._linkSids);
    }

    public void assign(BBind.Data _o_) {
        _modules.clear();
        for (var _e_ : _o_._modules.entrySet())
            _modules.put(_e_.getKey(), _e_.getValue().copy());
        _linkSids.clear();
        _linkSids.addAll(_o_._linkSids);
    }

    @Override
    public BBind.Data copy() {
        var _c_ = new BBind.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BBind.Data _a_, BBind.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
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
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("Zeze.Builtin.Provider.BBind: {\n");
        _s_.append(_i1_).append("modules={");
        if (!_modules.isEmpty()) {
            _s_.append('\n');
            for (var _e_ : _modules.entrySet()) {
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("linkSids={");
        if (!_linkSids.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _linkSids) {
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("}\n");
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
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
