// auto-generated @formatter:off
package demo.Module1;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BSimple extends Zeze.Transaction.Bean implements BSimpleReadOnly {
    public static final long TYPEID = 4513771153805810055L;

    private int _int_1; // com aa
    private long _long2; // com aa
    private String _string3; // com aa
    private final Zeze.Transaction.Collections.CollOne<demo.Module1.BRemoved2> _removed; // com aa
    private static final Zeze.Transaction.Collections.LogOneMeta<demo.Module1.BRemoved2> meta1_removed
            = Zeze.Transaction.Collections.LogOneMeta.get(demo.Module1.BRemoved2.class);

    private transient @Nullable Object __zeze_map_key__;

    @Override
    public @Nullable Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object _v_) {
        __zeze_map_key__ = _v_;
    }

    private static final java.lang.invoke.VarHandle vh_int_1;
    private static final java.lang.invoke.VarHandle vh_long2;
    private static final java.lang.invoke.VarHandle vh_string3;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_int_1 = _l_.findVarHandle(BSimple.class, "_int_1", int.class);
            vh_long2 = _l_.findVarHandle(BSimple.class, "_long2", long.class);
            vh_string3 = _l_.findVarHandle(BSimple.class, "_string3", String.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getInt_1() {
        if (!isManaged())
            return _int_1;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _int_1;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _int_1;
    }

    public void setInt_1(int _v_) {
        if (!isManaged()) {
            _int_1 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_int_1, _v_));
    }

    @Override
    public long getLong2() {
        if (!isManaged())
            return _long2;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _long2;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _long2;
    }

    public void setLong2(long _v_) {
        if (!isManaged()) {
            _long2 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_long2, _v_));
    }

    @Override
    public String getString3() {
        if (!isManaged())
            return _string3;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _string3;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 3);
        return log != null ? log.stringValue() : _string3;
    }

    public void setString3(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _string3 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 3, vh_string3, _v_));
    }

    public demo.Module1.BRemoved2 getRemoved() {
        return _removed.getValue();
    }

    public void setRemoved(demo.Module1.BRemoved2 _v_) {
        _removed.setValue(_v_);
    }

    @Override
    public demo.Module1.BRemoved2ReadOnly getRemovedReadOnly() {
        return _removed.getValue();
    }

    @SuppressWarnings("deprecation")
    public BSimple() {
        _string3 = "";
        _removed = new Zeze.Transaction.Collections.CollOne<>(new demo.Module1.BRemoved2(), meta1_removed);
        _removed.variableId(4);
    }

    @SuppressWarnings("deprecation")
    public BSimple(int _int_1_, long _long2_, @Nullable String _string3_) {
        _int_1 = _int_1_;
        _long2 = _long2_;
        if (_string3_ == null)
            _string3_ = "";
        _string3 = _string3_;
        _removed = new Zeze.Transaction.Collections.CollOne<>(new demo.Module1.BRemoved2(), meta1_removed);
        _removed.variableId(4);
    }

    @Override
    public void reset() {
        setInt_1(0);
        setLong2(0);
        setString3("");
        _removed.reset();
        _unknown_ = null;
    }

    @Override
    public demo.Module1.BSimple.Data toData() {
        var _d_ = new demo.Module1.BSimple.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((demo.Module1.BSimple.Data)_o_);
    }

    public void assign(BSimple.Data _o_) {
        setInt_1(_o_._int_1);
        setLong2(_o_._long2);
        setString3(_o_._string3);
        var _d__removed = new demo.Module1.BRemoved2();
        _d__removed.assign(_o_._removed);
        _removed.setValue(_d__removed);
        _unknown_ = null;
    }

    public void assign(BSimple _o_) {
        setInt_1(_o_.getInt_1());
        setLong2(_o_.getLong2());
        setString3(_o_.getString3());
        _removed.assign(_o_._removed);
        _unknown_ = _o_._unknown_;
    }

    public BSimple copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSimple copy() {
        var _c_ = new BSimple();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSimple _a_, BSimple _b_) {
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
        _s_.append("demo.Module1.BSimple: {\n");
        _s_.append(_i1_).append("int_1=").append(getInt_1()).append(",\n");
        _s_.append(_i1_).append("long2=").append(getLong2()).append(",\n");
        _s_.append(_i1_).append("string3=").append(getString3()).append(",\n");
        _s_.append(_i1_).append("removed=");
        _removed.buildString(_s_, _l_ + 8);
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
            int _x_ = getInt_1();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getLong2();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getString3();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 4, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _removed.encode(_o_);
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
            setInt_1(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setLong2(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setString3(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _o_.ReadBean(_removed, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BSimple))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSimple)_o_;
        if (getInt_1() != _b_.getInt_1())
            return false;
        if (getLong2() != _b_.getLong2())
            return false;
        if (!getString3().equals(_b_.getString3()))
            return false;
        if (!_removed.equals(_b_._removed))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int _p_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _p_ + Integer.hashCode(getInt_1());
        _h_ = _h_ * _p_ + Long.hashCode(getLong2());
        _h_ = _h_ * _p_ + getString3().hashCode();
        _h_ = _h_ * _p_ + _removed.hashCode();
        return _h_;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _removed.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _removed.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getInt_1() < 0)
            return true;
        if (getLong2() < 0)
            return true;
        if (_removed.negativeCheck())
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
                case 1: _int_1 = _v_.intValue(); break;
                case 2: _long2 = _v_.longValue(); break;
                case 3: _string3 = _v_.stringValue(); break;
                case 4: _removed.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setInt_1(_r_.getInt(_pn_ + "int_1"));
        setLong2(_r_.getLong(_pn_ + "long2"));
        setString3(_r_.getString(_pn_ + "string3"));
        if (getString3() == null)
            setString3("");
        _p_.add("removed");
        _removed.decodeResultSet(_p_, _r_);
        _p_.removeLast();
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "int_1", getInt_1());
        _s_.appendLong(_pn_ + "long2", getLong2());
        _s_.appendString(_pn_ + "string3", getString3());
        _p_.add("removed");
        _removed.encodeSQLStatement(_p_, _s_);
        _p_.removeLast();
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "int_1", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "long2", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "string3", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "removed", "demo.Module1.BRemoved2", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 4513771153805810055L;

    private int _int_1; // com aa
    private long _long2; // com aa
    private String _string3; // com aa
    private demo.Module1.BRemoved2.Data _removed; // com aa

    public int getInt_1() {
        return _int_1;
    }

    public void setInt_1(int _v_) {
        _int_1 = _v_;
    }

    public long getLong2() {
        return _long2;
    }

    public void setLong2(long _v_) {
        _long2 = _v_;
    }

    public String getString3() {
        return _string3;
    }

    public void setString3(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _string3 = _v_;
    }

    public demo.Module1.BRemoved2.Data getRemoved() {
        return _removed;
    }

    public void setRemoved(demo.Module1.BRemoved2.Data _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _removed = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _string3 = "";
        _removed = new demo.Module1.BRemoved2.Data();
    }

    @SuppressWarnings("deprecation")
    public Data(int _int_1_, long _long2_, @Nullable String _string3_, demo.Module1.BRemoved2.@Nullable Data _removed_) {
        _int_1 = _int_1_;
        _long2 = _long2_;
        if (_string3_ == null)
            _string3_ = "";
        _string3 = _string3_;
        if (_removed_ == null)
            _removed_ = new demo.Module1.BRemoved2.Data();
        _removed = _removed_;
    }

    @Override
    public void reset() {
        _int_1 = 0;
        _long2 = 0;
        _string3 = "";
        _removed.reset();
    }

    @Override
    public demo.Module1.BSimple toBean() {
        var _b_ = new demo.Module1.BSimple();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BSimple)_o_);
    }

    public void assign(BSimple _o_) {
        _int_1 = _o_.getInt_1();
        _long2 = _o_.getLong2();
        _string3 = _o_.getString3();
        _removed.assign(_o_._removed.getValue());
    }

    public void assign(BSimple.Data _o_) {
        _int_1 = _o_._int_1;
        _long2 = _o_._long2;
        _string3 = _o_._string3;
        _removed.assign(_o_._removed);
    }

    @Override
    public BSimple.Data copy() {
        var _c_ = new BSimple.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSimple.Data _a_, BSimple.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BSimple.Data clone() {
        return (BSimple.Data)super.clone();
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
        _s_.append("demo.Module1.BSimple: {\n");
        _s_.append(_i1_).append("int_1=").append(_int_1).append(",\n");
        _s_.append(_i1_).append("long2=").append(_long2).append(",\n");
        _s_.append(_i1_).append("string3=").append(_string3).append(",\n");
        _s_.append(_i1_).append("removed=");
        _removed.buildString(_s_, _l_ + 8);
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
            int _x_ = _int_1;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = _long2;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = _string3;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 4, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _removed.encode(_o_);
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
            _int_1 = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _long2 = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _string3 = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _o_.ReadBean(_removed, _t_);
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
        if (!(_o_ instanceof BSimple.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSimple.Data)_o_;
        if (_int_1 != _b_._int_1)
            return false;
        if (_long2 != _b_._long2)
            return false;
        if (!_string3.equals(_b_._string3))
            return false;
        if (!_removed.equals(_b_._removed))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int _p_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _p_ + Integer.hashCode(_int_1);
        _h_ = _h_ * _p_ + Long.hashCode(_long2);
        _h_ = _h_ * _p_ + _string3.hashCode();
        _h_ = _h_ * _p_ + _removed.hashCode();
        return _h_;
    }
}
}
