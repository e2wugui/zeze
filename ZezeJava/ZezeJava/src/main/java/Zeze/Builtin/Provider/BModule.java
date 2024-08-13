// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// gs to link
@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BModule extends Zeze.Transaction.Bean implements BModuleReadOnly {
    public static final long TYPEID = 5883923521926593765L;

    public static final int ChoiceTypeDefault = 0; // 默认是ChoiceTypeRequest
    public static final int ChoiceTypeHashAccount = 1; // 按账号名的一致性hash选取
    public static final int ChoiceTypeHashRoleId = 2; // 按角色ID的一致性hash选取
    public static final int ChoiceTypeFeedFullOneByOne = 3; // 使用全局迭代器选取符合条件的(有效,非过载,非超限,匹配版本)
    public static final int ChoiceTypeHashSourceAddress = 4; // 按来源IP地址端口的一致性hash选取
    public static final int ChoiceTypeLoad = 5; // 从符合条件的(有效,非过载,匹配版本)里面以剩余承载量为权重选取
    public static final int ChoiceTypeRequest = 6; // 从符合条件的(有效,非过载,匹配版本)里面以最近5秒请求量反比为权重选取
    public static final int ConfigTypeDefault = 0;
    public static final int ConfigTypeSpecial = 1;
    public static final int ConfigTypeDynamic = 2;

    private int _ChoiceType;
    private int _ConfigType;

    private transient Object __zeze_map_key__;

    @Override
    public Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object _v_) {
        __zeze_map_key__ = _v_;
    }

    private static final java.lang.invoke.VarHandle vh_ChoiceType;
    private static final java.lang.invoke.VarHandle vh_ConfigType;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_ChoiceType = _l_.findVarHandle(BModule.class, "_ChoiceType", int.class);
            vh_ConfigType = _l_.findVarHandle(BModule.class, "_ConfigType", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getChoiceType() {
        if (!isManaged())
            return _ChoiceType;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ChoiceType;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _ChoiceType;
    }

    public void setChoiceType(int _v_) {
        if (!isManaged()) {
            _ChoiceType = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_ChoiceType, _v_));
    }

    @Override
    public int getConfigType() {
        if (!isManaged())
            return _ConfigType;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ConfigType;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _ConfigType;
    }

    public void setConfigType(int _v_) {
        if (!isManaged()) {
            _ConfigType = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 2, vh_ConfigType, _v_));
    }

    @SuppressWarnings("deprecation")
    public BModule() {
    }

    @SuppressWarnings("deprecation")
    public BModule(int _ChoiceType_, int _ConfigType_) {
        _ChoiceType = _ChoiceType_;
        _ConfigType = _ConfigType_;
    }

    @Override
    public void reset() {
        setChoiceType(0);
        setConfigType(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Provider.BModule.Data toData() {
        var _d_ = new Zeze.Builtin.Provider.BModule.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Provider.BModule.Data)_o_);
    }

    public void assign(BModule.Data _o_) {
        setChoiceType(_o_._ChoiceType);
        setConfigType(_o_._ConfigType);
        _unknown_ = null;
    }

    public void assign(BModule _o_) {
        setChoiceType(_o_.getChoiceType());
        setConfigType(_o_.getConfigType());
        _unknown_ = _o_._unknown_;
    }

    public BModule copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BModule copy() {
        var _c_ = new BModule();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BModule _a_, BModule _b_) {
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
        _s_.append("Zeze.Builtin.Provider.BModule: {\n");
        _s_.append(_i1_).append("ChoiceType=").append(getChoiceType()).append(",\n");
        _s_.append(_i1_).append("ConfigType=").append(getConfigType()).append('\n');
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
            int _x_ = getChoiceType();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getConfigType();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
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
            setChoiceType(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setConfigType(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BModule))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BModule)_o_;
        if (getChoiceType() != _b_.getChoiceType())
            return false;
        if (getConfigType() != _b_.getConfigType())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getChoiceType() < 0)
            return true;
        if (getConfigType() < 0)
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
                case 1: _ChoiceType = _v_.intValue(); break;
                case 2: _ConfigType = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setChoiceType(_r_.getInt(_pn_ + "ChoiceType"));
        setConfigType(_r_.getInt(_pn_ + "ConfigType"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "ChoiceType", getChoiceType());
        _s_.appendInt(_pn_ + "ConfigType", getConfigType());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ChoiceType", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ConfigType", "int", "", ""));
        return _v_;
    }

// gs to link
@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 5883923521926593765L;

    public static final int ChoiceTypeDefault = 0; // 默认是ChoiceTypeRequest
    public static final int ChoiceTypeHashAccount = 1; // 按账号名的一致性hash选取
    public static final int ChoiceTypeHashRoleId = 2; // 按角色ID的一致性hash选取
    public static final int ChoiceTypeFeedFullOneByOne = 3; // 使用全局迭代器选取符合条件的(有效,非过载,非超限,匹配版本)
    public static final int ChoiceTypeHashSourceAddress = 4; // 按来源IP地址端口的一致性hash选取
    public static final int ChoiceTypeLoad = 5; // 从符合条件的(有效,非过载,匹配版本)里面以剩余承载量为权重选取
    public static final int ChoiceTypeRequest = 6; // 从符合条件的(有效,非过载,匹配版本)里面以最近5秒请求量反比为权重选取
    public static final int ConfigTypeDefault = 0;
    public static final int ConfigTypeSpecial = 1;
    public static final int ConfigTypeDynamic = 2;

    private int _ChoiceType;
    private int _ConfigType;

    public int getChoiceType() {
        return _ChoiceType;
    }

    public void setChoiceType(int _v_) {
        _ChoiceType = _v_;
    }

    public int getConfigType() {
        return _ConfigType;
    }

    public void setConfigType(int _v_) {
        _ConfigType = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(int _ChoiceType_, int _ConfigType_) {
        _ChoiceType = _ChoiceType_;
        _ConfigType = _ConfigType_;
    }

    @Override
    public void reset() {
        _ChoiceType = 0;
        _ConfigType = 0;
    }

    @Override
    public Zeze.Builtin.Provider.BModule toBean() {
        var _b_ = new Zeze.Builtin.Provider.BModule();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BModule)_o_);
    }

    public void assign(BModule _o_) {
        _ChoiceType = _o_.getChoiceType();
        _ConfigType = _o_.getConfigType();
    }

    public void assign(BModule.Data _o_) {
        _ChoiceType = _o_._ChoiceType;
        _ConfigType = _o_._ConfigType;
    }

    @Override
    public BModule.Data copy() {
        var _c_ = new BModule.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BModule.Data _a_, BModule.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BModule.Data clone() {
        return (BModule.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Provider.BModule: {\n");
        _s_.append(_i1_).append("ChoiceType=").append(_ChoiceType).append(",\n");
        _s_.append(_i1_).append("ConfigType=").append(_ConfigType).append('\n');
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
            int _x_ = _ChoiceType;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _ConfigType;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _ChoiceType = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _ConfigType = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BModule.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BModule.Data)_o_;
        if (_ChoiceType != _b_._ChoiceType)
            return false;
        if (_ConfigType != _b_._ConfigType)
            return false;
        return true;
    }
}
}
