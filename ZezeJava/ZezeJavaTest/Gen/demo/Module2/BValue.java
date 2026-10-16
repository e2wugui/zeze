// auto-generated @formatter:off
package demo.Module2;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BValue extends Zeze.Transaction.Bean implements BValueReadOnly {
    public static final long TYPEID = 3672513970861237847L;

    private int _s; // com aa
    private final Zeze.Transaction.Collections.PList1<Integer> _plist; // com aa
    private static final Zeze.Transaction.Collections.List1Meta<Integer> meta1_plist
            = Zeze.Transaction.Collections.List1Meta.get(Integer.class);

    private final Zeze.Transaction.Collections.PSortedMap1<Integer, Integer> _psortedmap; // com aa
    private static final Zeze.Transaction.Collections.SortedMap1Meta<Integer, Integer> meta2_psortedmap
            = Zeze.Transaction.Collections.SortedMap1Meta.get(Integer.class, Integer.class);

    private transient @Nullable Object __zeze_map_key__;

    @Override
    public @Nullable Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object _v_) {
        __zeze_map_key__ = _v_;
    }

    private static final java.lang.invoke.VarHandle vh_s;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_s = _l_.findVarHandle(BValue.class, "_s", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getS() {
        if (!isManaged())
            return _s;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _s;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _s;
    }

    public void setS(int _v_) {
        if (!isManaged()) {
            _s = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_s, _v_));
    }

    public Zeze.Transaction.Collections.PList1<Integer> getPlist() {
        return _plist;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Integer> getPlistReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_plist);
    }

    public Zeze.Transaction.Collections.PSortedMap1<Integer, Integer> getPsortedmap() {
        return _psortedmap;
    }

    @Override
    public Zeze.Transaction.Collections.PSortedMap1ReadOnly<Integer, Integer> getPsortedmapReadOnly() {
        return new Zeze.Transaction.Collections.PSortedMap1ReadOnly<>(_psortedmap);
    }

    @SuppressWarnings("deprecation")
    public BValue() {
        _s = 1;
        _plist = new Zeze.Transaction.Collections.PList1<>(meta1_plist);
        _plist.variableId(2);
        _psortedmap = new Zeze.Transaction.Collections.PSortedMap1<>(meta2_psortedmap);
        _psortedmap.variableId(3);
    }

    @SuppressWarnings("deprecation")
    public BValue(int _s_) {
        _s = _s_;
        _plist = new Zeze.Transaction.Collections.PList1<>(meta1_plist);
        _plist.variableId(2);
        _psortedmap = new Zeze.Transaction.Collections.PSortedMap1<>(meta2_psortedmap);
        _psortedmap.variableId(3);
    }

    @Override
    public void reset() {
        setS(1);
        _plist.clear();
        _psortedmap.clear();
        _unknown_ = null;
    }

    @Override
    public demo.Module2.BValue.Data toData() {
        var _d_ = new demo.Module2.BValue.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((demo.Module2.BValue.Data)_o_);
    }

    public void assign(BValue.Data _o_) {
        setS(_o_._s);
        _plist.clear();
        _plist.addAll(_o_._plist);
        _psortedmap.clear();
        _psortedmap.putAll(_o_._psortedmap);
        _unknown_ = null;
    }

    public void assign(BValue _o_) {
        setS(_o_.getS());
        _plist.assign(_o_._plist);
        _psortedmap.assign(_o_._psortedmap);
        _unknown_ = _o_._unknown_;
    }

    public BValue copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BValue copy() {
        var _c_ = new BValue();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BValue _a_, BValue _b_) {
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
        _s_.append("demo.Module2.BValue: {\n");
        _s_.append(_i1_).append("s=").append(getS()).append(",\n");
        _s_.append(_i1_).append("plist=[");
        if (!_plist.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _plist) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_plist.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("psortedmap={");
        if (!_psortedmap.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _psortedmap.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_psortedmap.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=").append(_e_.getValue()).append(",\n");
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

    private byte @Nullable [] _unknown_;

    public byte @Nullable [] unknown() {
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
            int _x_ = getS();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _plist;
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
        {
            var _x_ = _psortedmap;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.INTEGER);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _o_.WriteLong(_e_.getValue());
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
            setS(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else
            setS(0);
        if (_i_ == 2) {
            var _x_ = _plist;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _psortedmap;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadInt(_t_);
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
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BValue))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BValue)_o_;
        if (getS() != _b_.getS())
            return false;
        if (!_plist.equals(_b_._plist))
            return false;
        if (!_psortedmap.equals(_b_._psortedmap))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _plist.initRootInfo(_r_, this);
        _psortedmap.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _plist.initRootInfoWithRedo(_r_, this);
        _psortedmap.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getS() < 0)
            return true;
        for (var _v_ : _plist) {
            if (_v_ < 0)
                return true;
        }
        for (var _v_ : _psortedmap.values()) {
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
                case 1: _s = _v_.intValue(); break;
                case 2: _plist.followerApply(_v_); break;
                case 3: _psortedmap.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setS(_r_.getInt(_pn_ + "s"));
        Zeze.Serialize.Helper.decodeJsonList(_plist, Integer.class, _r_.getString(_pn_ + "plist"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "psortedmap", _psortedmap, _r_.getString(_pn_ + "psortedmap"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "s", getS());
        _s_.appendString(_pn_ + "plist", Zeze.Serialize.Helper.encodeJson(_plist));
        _s_.appendString(_pn_ + "psortedmap", Zeze.Serialize.Helper.encodeJson(_psortedmap));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "s", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "plist", "list", "", "int"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "psortedmap", "sortedmap", "int", "int"));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 3672513970861237847L;

    private int _s; // com aa
    private java.util.ArrayList<Integer> _plist; // com aa
    private java.util.TreeMap<Integer, Integer> _psortedmap; // com aa

    public int getS() {
        return _s;
    }

    public void setS(int _v_) {
        _s = _v_;
    }

    public java.util.ArrayList<Integer> getPlist() {
        return _plist;
    }

    public void setPlist(java.util.ArrayList<Integer> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _plist = _v_;
    }

    public java.util.TreeMap<Integer, Integer> getPsortedmap() {
        return _psortedmap;
    }

    public void setPsortedmap(java.util.TreeMap<Integer, Integer> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _psortedmap = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _s = 1;
        _plist = new java.util.ArrayList<>();
        _psortedmap = new java.util.TreeMap<>();
    }

    @SuppressWarnings("deprecation")
    public Data(int _s_, java.util.@Nullable ArrayList<Integer> _plist_, java.util.@Nullable TreeMap<Integer, Integer> _psortedmap_) {
        _s = _s_;
        if (_plist_ == null)
            _plist_ = new java.util.ArrayList<>();
        _plist = _plist_;
        if (_psortedmap_ == null)
            _psortedmap_ = new java.util.TreeMap<>();
        _psortedmap = _psortedmap_;
    }

    @Override
    public void reset() {
        _s = 1;
        _plist.clear();
        _psortedmap.clear();
    }

    @Override
    public demo.Module2.BValue toBean() {
        var _b_ = new demo.Module2.BValue();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BValue)_o_);
    }

    public void assign(BValue _o_) {
        _s = _o_.getS();
        _plist.clear();
        _plist.addAll(_o_._plist);
        _psortedmap.clear();
        _psortedmap.putAll(_o_._psortedmap);
    }

    public void assign(BValue.Data _o_) {
        _s = _o_._s;
        _plist.clear();
        _plist.addAll(_o_._plist);
        _psortedmap.clear();
        _psortedmap.putAll(_o_._psortedmap);
    }

    @Override
    public BValue.Data copy() {
        var _c_ = new BValue.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BValue.Data _a_, BValue.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BValue.Data clone() {
        return (BValue.Data)super.clone();
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
        _s_.append("demo.Module2.BValue: {\n");
        _s_.append(_i1_).append("s=").append(_s).append(",\n");
        _s_.append(_i1_).append("plist=[");
        if (!_plist.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _plist) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_plist.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("psortedmap={");
        if (!_psortedmap.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _psortedmap.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_psortedmap.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=").append(_e_.getValue()).append(",\n");
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
            int _x_ = _s;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _plist;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (int _j_ = 0, _c_ = _x_.size(); _j_ < _c_; _j_++) {
                    var _v_ = _x_.get(_j_);
                    _o_.WriteLong(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _psortedmap;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.INTEGER);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _o_.WriteLong(_e_.getValue());
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
            _s = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else
            _s = 0;
        if (_i_ == 2) {
            var _x_ = _plist;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _psortedmap;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadInt(_t_);
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

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BValue.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BValue.Data)_o_;
        if (_s != _b_._s)
            return false;
        if (!_plist.equals(_b_._plist))
            return false;
        if (!_psortedmap.equals(_b_._psortedmap))
            return false;
        return true;
    }
}
}
