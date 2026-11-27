// auto-generated @formatter:off
package rocksdynamic.Fixture;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class ExternalKeyDefaults extends Zeze.Transaction.Bean implements ExternalKeyDefaultsReadOnly {
    public static final long TYPEID = -8694629119398866989L;

    private Zeze.Util.Id128 _KeyValue;

    private static final Zeze.Transaction.Collections.BeanKeyMeta<Zeze.Util.Id128> meta_KeyValue
            = Zeze.Transaction.Collections.BeanKeyMeta.get(Zeze.Util.Id128.class);

    private static final java.lang.invoke.VarHandle vh_KeyValue;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_KeyValue = _l_.findVarHandle(ExternalKeyDefaults.class, "_KeyValue", Zeze.Util.Id128.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public Zeze.Util.Id128 getKeyValue() {
        if (!isManaged())
            return _KeyValue;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _KeyValue;
        @SuppressWarnings("unchecked")
        var log = (Zeze.Transaction.Logs.LogBeanKey<Zeze.Util.Id128>)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _KeyValue;
    }

    public void setKeyValue(Zeze.Util.Id128 _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _KeyValue = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBeanKey<>(this, 1, vh_KeyValue, meta_KeyValue, _v_));
    }

    @SuppressWarnings("deprecation")
    public ExternalKeyDefaults() {
        _KeyValue = new Zeze.Util.Id128(1L, 2L);
    }

    @SuppressWarnings("deprecation")
    public ExternalKeyDefaults(Zeze.Util.@Nullable Id128 _KeyValue_) {
        if (_KeyValue_ == null)
            _KeyValue_ = new Zeze.Util.Id128(1L, 2L);
        _KeyValue = _KeyValue_;
    }

    @Override
    public void reset() {
        setKeyValue(new Zeze.Util.Id128(1L, 2L));
        _unknown_ = null;
    }

    @Override
    public rocksdynamic.Fixture.ExternalKeyDefaults.Data toData() {
        var _d_ = new rocksdynamic.Fixture.ExternalKeyDefaults.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((rocksdynamic.Fixture.ExternalKeyDefaults.Data)_o_);
    }

    public void assign(ExternalKeyDefaults.Data _o_) {
        setKeyValue(_o_._KeyValue);
        _unknown_ = null;
    }

    public void assign(ExternalKeyDefaults _o_) {
        setKeyValue(_o_.getKeyValue());
        _unknown_ = _o_._unknown_;
    }

    public ExternalKeyDefaults copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public ExternalKeyDefaults copy() {
        var _c_ = new ExternalKeyDefaults();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(ExternalKeyDefaults _a_, ExternalKeyDefaults _b_) {
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
        _s_.append("rocksdynamic.Fixture.ExternalKeyDefaults: {\n");
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
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
            setKeyValue(_o_.ReadBeanKey(_t_, Zeze.Util.Id128.class, Zeze.Util.Id128::readFrom, () -> new Zeze.Util.Id128(1L, 2L)));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else
            setKeyValue(ByteBuffer.readEmptyBean(Zeze.Util.Id128::readFrom));
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof ExternalKeyDefaults))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (ExternalKeyDefaults)_o_;
        if (!getKeyValue().equals(_b_.getKeyValue()))
            return false;
        return true;
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
                case 1: _KeyValue = ((Zeze.Transaction.Logs.LogBeanKeyValue<Zeze.Util.Id128>)_v_).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        _p_.add("KeyValue");
        try {
            setKeyValue(Zeze.Util.Id128.readFromResultSet(_p_, _r_));
        } finally {
            _p_.removeLast();
        }
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        _p_.add("KeyValue");
        getKeyValue().encodeSQLStatement(_p_, _s_);
        _p_.removeLast();
    }

    @Override
    public java.util.ArrayList<Zeze.Transaction.VariableMeta> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Transaction.VariableMeta(1, "KeyValue", "Zeze.Util.Id128", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -8694629119398866989L;

    private Zeze.Util.Id128 _KeyValue;

    public Zeze.Util.Id128 getKeyValue() {
        return _KeyValue;
    }

    public void setKeyValue(Zeze.Util.Id128 _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _KeyValue = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _KeyValue = new Zeze.Util.Id128(1L, 2L);
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Util.@Nullable Id128 _KeyValue_) {
        if (_KeyValue_ == null)
            _KeyValue_ = new Zeze.Util.Id128(1L, 2L);
        _KeyValue = _KeyValue_;
    }

    @Override
    public void reset() {
        _KeyValue = new Zeze.Util.Id128(1L, 2L);
    }

    @Override
    public rocksdynamic.Fixture.ExternalKeyDefaults toBean() {
        var _b_ = new rocksdynamic.Fixture.ExternalKeyDefaults();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((ExternalKeyDefaults)_o_);
    }

    public void assign(ExternalKeyDefaults _o_) {
        _KeyValue = _o_.getKeyValue();
    }

    public void assign(ExternalKeyDefaults.Data _o_) {
        _KeyValue = _o_._KeyValue;
    }

    @Override
    public ExternalKeyDefaults.Data copy() {
        var _c_ = new ExternalKeyDefaults.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(ExternalKeyDefaults.Data _a_, ExternalKeyDefaults.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public ExternalKeyDefaults.Data clone() {
        return (ExternalKeyDefaults.Data)super.clone();
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
        _s_.append("rocksdynamic.Fixture.ExternalKeyDefaults: {\n");
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
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
            _KeyValue = _o_.ReadBeanKey(_t_, Zeze.Util.Id128.class, Zeze.Util.Id128::readFrom, () -> new Zeze.Util.Id128(1L, 2L));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else
            _KeyValue = ByteBuffer.readEmptyBean(Zeze.Util.Id128::readFrom);
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof ExternalKeyDefaults.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (ExternalKeyDefaults.Data)_o_;
        if (!_KeyValue.equals(_b_._KeyValue))
            return false;
        return true;
    }
}
}
