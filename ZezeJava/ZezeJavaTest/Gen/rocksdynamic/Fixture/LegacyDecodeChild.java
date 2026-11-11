// auto-generated @formatter:off
package rocksdynamic.Fixture;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class LegacyDecodeChild extends Zeze.Transaction.Bean implements LegacyDecodeChildReadOnly {
    public static final long TYPEID = 2148062530864543988L;

    private int _Number;
    private int _Scratch;
    private final Zeze.Transaction.Collections.PList1<Integer> _Entries;
    private static final Zeze.Transaction.Collections.List1Meta<Integer> meta1_Entries
            = Zeze.Transaction.Collections.List1Meta.get(Integer.class);

    private rocksdynamic.Fixture.LegacyDecodeKey _KeyValue;

    private static final Zeze.Transaction.Collections.BeanKeyMeta<rocksdynamic.Fixture.LegacyDecodeKey> meta_KeyValue
            = Zeze.Transaction.Collections.BeanKeyMeta.get(rocksdynamic.Fixture.LegacyDecodeKey.class);

    private static final java.lang.invoke.VarHandle vh_Number;
    private static final java.lang.invoke.VarHandle vh_Scratch;
    private static final java.lang.invoke.VarHandle vh_KeyValue;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Number = _l_.findVarHandle(LegacyDecodeChild.class, "_Number", int.class);
            vh_Scratch = _l_.findVarHandle(LegacyDecodeChild.class, "_Scratch", int.class);
            vh_KeyValue = _l_.findVarHandle(LegacyDecodeChild.class, "_KeyValue", rocksdynamic.Fixture.LegacyDecodeKey.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getNumber() {
        if (!isManaged())
            return _Number;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Number;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Number;
    }

    public void setNumber(int _v_) {
        if (!isManaged()) {
            _Number = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_Number, _v_));
    }

    @Override
    public int getScratch() {
        if (!isManaged())
            return _Scratch;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Scratch;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Scratch;
    }

    public void setScratch(int _v_) {
        if (!isManaged()) {
            _Scratch = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 2, vh_Scratch, _v_));
    }

    public Zeze.Transaction.Collections.PList1<Integer> getEntries() {
        return _Entries;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Integer> getEntriesReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_Entries);
    }

    @Override
    public rocksdynamic.Fixture.LegacyDecodeKey getKeyValue() {
        if (!isManaged())
            return _KeyValue;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _KeyValue;
        @SuppressWarnings("unchecked")
        var log = (Zeze.Transaction.Logs.LogBeanKey<rocksdynamic.Fixture.LegacyDecodeKey>)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _KeyValue;
    }

    public void setKeyValue(rocksdynamic.Fixture.LegacyDecodeKey _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _KeyValue = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBeanKey<>(this, 4, vh_KeyValue, meta_KeyValue, _v_));
    }

    @SuppressWarnings("deprecation")
    public LegacyDecodeChild() {
        _Number = 7;
        _Entries = new Zeze.Transaction.Collections.PList1<>(meta1_Entries);
        _Entries.variableId(3);
        _KeyValue = new rocksdynamic.Fixture.LegacyDecodeKey();
    }

    @SuppressWarnings("deprecation")
    public LegacyDecodeChild(int _Number_, int _Scratch_, rocksdynamic.Fixture.@Nullable LegacyDecodeKey _KeyValue_) {
        _Number = _Number_;
        _Scratch = _Scratch_;
        _Entries = new Zeze.Transaction.Collections.PList1<>(meta1_Entries);
        _Entries.variableId(3);
        if (_KeyValue_ == null)
            _KeyValue_ = new rocksdynamic.Fixture.LegacyDecodeKey();
        _KeyValue = _KeyValue_;
    }

    @Override
    public void reset() {
        setNumber(7);
        setScratch(0);
        _Entries.clear();
        setKeyValue(new rocksdynamic.Fixture.LegacyDecodeKey());
        _unknown_ = null;
    }

    @Override
    public rocksdynamic.Fixture.LegacyDecodeChild.Data toData() {
        var _d_ = new rocksdynamic.Fixture.LegacyDecodeChild.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((rocksdynamic.Fixture.LegacyDecodeChild.Data)_o_);
    }

    public void assign(LegacyDecodeChild.Data _o_) {
        setNumber(_o_._Number);
        setScratch(_o_._Scratch);
        _Entries.clear();
        _Entries.addAll(_o_._Entries);
        setKeyValue(_o_._KeyValue);
        _unknown_ = null;
    }

    public void assign(LegacyDecodeChild _o_) {
        setNumber(_o_.getNumber());
        setScratch(_o_.getScratch());
        _Entries.assign(_o_._Entries);
        setKeyValue(_o_.getKeyValue());
        _unknown_ = _o_._unknown_;
    }

    public LegacyDecodeChild copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public LegacyDecodeChild copy() {
        var _c_ = new LegacyDecodeChild();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(LegacyDecodeChild _a_, LegacyDecodeChild _b_) {
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
        _s_.append("rocksdynamic.Fixture.LegacyDecodeChild: {\n");
        _s_.append(_i1_).append("Number=").append(getNumber()).append(",\n");
        _s_.append(_i1_).append("Scratch=").append(getScratch()).append(",\n");
        _s_.append(_i1_).append("Entries=[");
        if (!_Entries.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _Entries) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Entries.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("KeyValue=");
        getKeyValue().buildString(_s_, _l_ + 8);
        _s_.append('\n');
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
            int _x_ = getNumber();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        while (_ui_ < 3) {
            _i_ = _o_.writeUnknownField(_i_, _ui_, _u_);
            _ui_ = _u_.readUnknownIndex();
        }
        {
            var _x_ = _Entries;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 4, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getKeyValue().encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
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
            setNumber(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else
            setNumber(0);
        while ((_t_ & 0xff) > 1 && _i_ < 3) {
            _u_ = _o_.readUnknownField(_i_, _t_, _u_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _Entries;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else
            _Entries.clear();
        if (_i_ == 4) {
            setKeyValue(_o_.ReadBean(new rocksdynamic.Fixture.LegacyDecodeKey(), _t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else
            setKeyValue(new rocksdynamic.Fixture.LegacyDecodeKey());
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    public void restoreOmittedFields(LegacyDecodeChild _defaults_) {
        setNumber(_defaults_.getNumber());
        _Entries.clear();
        setKeyValue(_defaults_.getKeyValue());
        _unknown_ = null;
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof LegacyDecodeChild))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (LegacyDecodeChild)_o_;
        if (getNumber() != _b_.getNumber())
            return false;
        if (getScratch() != _b_.getScratch())
            return false;
        if (!_Entries.equals(_b_._Entries))
            return false;
        if (!getKeyValue().equals(_b_.getKeyValue()))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Entries.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Entries.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getNumber() < 0)
            return true;
        if (getScratch() < 0)
            return true;
        for (var _v_ : _Entries) {
            if (_v_ < 0)
                return true;
        }
        if (getKeyValue().negativeCheck())
            return true;
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
                case 1: _Number = _v_.intValue(); break;
                case 3: _Entries.followerApply(_v_); break;
                case 4: _KeyValue = ((Zeze.Transaction.Logs.LogBeanKeyValue<rocksdynamic.Fixture.LegacyDecodeKey>)_v_).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setNumber(_r_.getInt(_pn_ + "Number"));
        Zeze.Serialize.Helper.decodeJsonList(_Entries, Integer.class, _r_.getString(_pn_ + "Entries"));
        _p_.add("KeyValue");
        getKeyValue().decodeResultSet(_p_, _r_);
        _p_.removeLast();
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "Number", getNumber());
        _s_.appendString(_pn_ + "Entries", Zeze.Serialize.Helper.encodeJson(_Entries));
        _p_.add("KeyValue");
        getKeyValue().encodeSQLStatement(_p_, _s_);
        _p_.removeLast();
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Number", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Scratch", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Entries", "list", "", "int"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "KeyValue", "rocksdynamic.Fixture.LegacyDecodeKey", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 2148062530864543988L;

    private int _Number;
    private int _Scratch;
    private java.util.ArrayList<Integer> _Entries;
    private rocksdynamic.Fixture.LegacyDecodeKey _KeyValue;

    public int getNumber() {
        return _Number;
    }

    public void setNumber(int _v_) {
        _Number = _v_;
    }

    public int getScratch() {
        return _Scratch;
    }

    public void setScratch(int _v_) {
        _Scratch = _v_;
    }

    public java.util.ArrayList<Integer> getEntries() {
        return _Entries;
    }

    public void setEntries(java.util.ArrayList<Integer> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Entries = _v_;
    }

    public rocksdynamic.Fixture.LegacyDecodeKey getKeyValue() {
        return _KeyValue;
    }

    public void setKeyValue(rocksdynamic.Fixture.LegacyDecodeKey _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _KeyValue = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Number = 7;
        _Entries = new java.util.ArrayList<>();
        _KeyValue = new rocksdynamic.Fixture.LegacyDecodeKey();
    }

    @SuppressWarnings("deprecation")
    public Data(int _Number_, int _Scratch_, java.util.@Nullable ArrayList<Integer> _Entries_, rocksdynamic.Fixture.@Nullable LegacyDecodeKey _KeyValue_) {
        _Number = _Number_;
        _Scratch = _Scratch_;
        if (_Entries_ == null)
            _Entries_ = new java.util.ArrayList<>();
        _Entries = _Entries_;
        if (_KeyValue_ == null)
            _KeyValue_ = new rocksdynamic.Fixture.LegacyDecodeKey();
        _KeyValue = _KeyValue_;
    }

    @Override
    public void reset() {
        _Number = 7;
        _Scratch = 0;
        _Entries.clear();
        _KeyValue = new rocksdynamic.Fixture.LegacyDecodeKey();
    }

    @Override
    public rocksdynamic.Fixture.LegacyDecodeChild toBean() {
        var _b_ = new rocksdynamic.Fixture.LegacyDecodeChild();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((LegacyDecodeChild)_o_);
    }

    public void assign(LegacyDecodeChild _o_) {
        _Number = _o_.getNumber();
        _Scratch = _o_.getScratch();
        _Entries.clear();
        _Entries.addAll(_o_._Entries);
        _KeyValue = _o_.getKeyValue();
    }

    public void assign(LegacyDecodeChild.Data _o_) {
        _Number = _o_._Number;
        _Scratch = _o_._Scratch;
        _Entries.clear();
        _Entries.addAll(_o_._Entries);
        _KeyValue = _o_._KeyValue;
    }

    @Override
    public LegacyDecodeChild.Data copy() {
        var _c_ = new LegacyDecodeChild.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(LegacyDecodeChild.Data _a_, LegacyDecodeChild.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public LegacyDecodeChild.Data clone() {
        return (LegacyDecodeChild.Data)super.clone();
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
        _s_.append("rocksdynamic.Fixture.LegacyDecodeChild: {\n");
        _s_.append(_i1_).append("Number=").append(_Number).append(",\n");
        _s_.append(_i1_).append("Scratch=").append(_Scratch).append(",\n");
        _s_.append(_i1_).append("Entries=[");
        if (!_Entries.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _Entries) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Entries.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("KeyValue=");
        _KeyValue.buildString(_s_, _l_ + 8);
        _s_.append('\n');
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
            int _x_ = _Number;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _Entries;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 4, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _KeyValue.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Number = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else
            _Number = 0;
        while ((_t_ & 0xff) > 1 && _i_ < 3) {
            _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _Entries;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else
            _Entries.clear();
        if (_i_ == 4) {
            _KeyValue = _o_.ReadBean(new rocksdynamic.Fixture.LegacyDecodeKey(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else
            _KeyValue = new rocksdynamic.Fixture.LegacyDecodeKey();
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    public void restoreOmittedFields(Data _defaults_) {
        _Number = _defaults_._Number;
        _Entries.clear();
        _Entries.addAll(_defaults_._Entries);
        _KeyValue = _defaults_._KeyValue;
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof LegacyDecodeChild.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (LegacyDecodeChild.Data)_o_;
        if (_Number != _b_._Number)
            return false;
        if (_Scratch != _b_._Scratch)
            return false;
        if (!_Entries.equals(_b_._Entries))
            return false;
        if (!_KeyValue.equals(_b_._KeyValue))
            return false;
        return true;
    }
}
}
