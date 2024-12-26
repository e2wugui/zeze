// auto-generated @formatter:off
package Zeze.Builtin.HotDistribute;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BVariable extends Zeze.Transaction.Bean implements BVariableReadOnly {
    public static final long TYPEID = 7877437207710416076L;

    private int _Id;
    private String _Name;
    private String _Type;
    private String _Key;
    private String _Value;

    private static final java.lang.invoke.VarHandle vh_Id;
    private static final java.lang.invoke.VarHandle vh_Name;
    private static final java.lang.invoke.VarHandle vh_Type;
    private static final java.lang.invoke.VarHandle vh_Key;
    private static final java.lang.invoke.VarHandle vh_Value;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Id = _l_.findVarHandle(BVariable.class, "_Id", int.class);
            vh_Name = _l_.findVarHandle(BVariable.class, "_Name", String.class);
            vh_Type = _l_.findVarHandle(BVariable.class, "_Type", String.class);
            vh_Key = _l_.findVarHandle(BVariable.class, "_Key", String.class);
            vh_Value = _l_.findVarHandle(BVariable.class, "_Value", String.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getId() {
        if (!isManaged())
            return _Id;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Id;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Id;
    }

    public void setId(int _v_) {
        if (!isManaged()) {
            _Id = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_Id, _v_));
    }

    @Override
    public String getName() {
        if (!isManaged())
            return _Name;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Name;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Name;
    }

    public void setName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Name = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 2, vh_Name, _v_));
    }

    @Override
    public String getType() {
        if (!isManaged())
            return _Type;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Type;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _Type;
    }

    public void setType(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Type = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 3, vh_Type, _v_));
    }

    @Override
    public String getKey() {
        if (!isManaged())
            return _Key;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Key;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _Key;
    }

    public void setKey(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Key = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 4, vh_Key, _v_));
    }

    @Override
    public String getValue() {
        if (!isManaged())
            return _Value;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Value;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _Value;
    }

    public void setValue(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Value = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 5, vh_Value, _v_));
    }

    @SuppressWarnings("deprecation")
    public BVariable() {
        _Name = "";
        _Type = "";
        _Key = "";
        _Value = "";
    }

    @SuppressWarnings("deprecation")
    public BVariable(int _Id_, String _Name_, String _Type_, String _Key_, String _Value_) {
        _Id = _Id_;
        if (_Name_ == null)
            _Name_ = "";
        _Name = _Name_;
        if (_Type_ == null)
            _Type_ = "";
        _Type = _Type_;
        if (_Key_ == null)
            _Key_ = "";
        _Key = _Key_;
        if (_Value_ == null)
            _Value_ = "";
        _Value = _Value_;
    }

    @Override
    public void reset() {
        setId(0);
        setName("");
        setType("");
        setKey("");
        setValue("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.HotDistribute.BVariable.Data toData() {
        var _d_ = new Zeze.Builtin.HotDistribute.BVariable.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.HotDistribute.BVariable.Data)_o_);
    }

    public void assign(BVariable.Data _o_) {
        setId(_o_._Id);
        setName(_o_._Name);
        setType(_o_._Type);
        setKey(_o_._Key);
        setValue(_o_._Value);
        _unknown_ = null;
    }

    public void assign(BVariable _o_) {
        setId(_o_.getId());
        setName(_o_.getName());
        setType(_o_.getType());
        setKey(_o_.getKey());
        setValue(_o_.getValue());
        _unknown_ = _o_._unknown_;
    }

    public BVariable copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BVariable copy() {
        var _c_ = new BVariable();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BVariable _a_, BVariable _b_) {
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
        _s_.append("Zeze.Builtin.HotDistribute.BVariable: {\n");
        _s_.append(_i1_).append("Id=").append(getId()).append(",\n");
        _s_.append(_i1_).append("Name=").append(getName()).append(",\n");
        _s_.append(_i1_).append("Type=").append(getType()).append(",\n");
        _s_.append(_i1_).append("Key=").append(getKey()).append(",\n");
        _s_.append(_i1_).append("Value=").append(getValue()).append('\n');
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
            int _x_ = getId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = getName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getType();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getKey();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getValue();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
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
            setId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setType(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setKey(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setValue(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BVariable))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BVariable)_o_;
        if (getId() != _b_.getId())
            return false;
        if (!getName().equals(_b_.getName()))
            return false;
        if (!getType().equals(_b_.getType()))
            return false;
        if (!getKey().equals(_b_.getKey()))
            return false;
        if (!getValue().equals(_b_.getValue()))
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getId() < 0)
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
                case 1: _Id = _v_.intValue(); break;
                case 2: _Name = _v_.stringValue(); break;
                case 3: _Type = _v_.stringValue(); break;
                case 4: _Key = _v_.stringValue(); break;
                case 5: _Value = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setId(_r_.getInt(_pn_ + "Id"));
        setName(_r_.getString(_pn_ + "Name"));
        if (getName() == null)
            setName("");
        setType(_r_.getString(_pn_ + "Type"));
        if (getType() == null)
            setType("");
        setKey(_r_.getString(_pn_ + "Key"));
        if (getKey() == null)
            setKey("");
        setValue(_r_.getString(_pn_ + "Value"));
        if (getValue() == null)
            setValue("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "Id", getId());
        _s_.appendString(_pn_ + "Name", getName());
        _s_.appendString(_pn_ + "Type", getType());
        _s_.appendString(_pn_ + "Key", getKey());
        _s_.appendString(_pn_ + "Value", getValue());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Id", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Name", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Type", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Key", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "Value", "string", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 7877437207710416076L;

    private int _Id;
    private String _Name;
    private String _Type;
    private String _Key;
    private String _Value;

    public int getId() {
        return _Id;
    }

    public void setId(int _v_) {
        _Id = _v_;
    }

    public String getName() {
        return _Name;
    }

    public void setName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Name = _v_;
    }

    public String getType() {
        return _Type;
    }

    public void setType(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Type = _v_;
    }

    public String getKey() {
        return _Key;
    }

    public void setKey(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Key = _v_;
    }

    public String getValue() {
        return _Value;
    }

    public void setValue(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Value = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Name = "";
        _Type = "";
        _Key = "";
        _Value = "";
    }

    @SuppressWarnings("deprecation")
    public Data(int _Id_, String _Name_, String _Type_, String _Key_, String _Value_) {
        _Id = _Id_;
        if (_Name_ == null)
            _Name_ = "";
        _Name = _Name_;
        if (_Type_ == null)
            _Type_ = "";
        _Type = _Type_;
        if (_Key_ == null)
            _Key_ = "";
        _Key = _Key_;
        if (_Value_ == null)
            _Value_ = "";
        _Value = _Value_;
    }

    @Override
    public void reset() {
        _Id = 0;
        _Name = "";
        _Type = "";
        _Key = "";
        _Value = "";
    }

    @Override
    public Zeze.Builtin.HotDistribute.BVariable toBean() {
        var _b_ = new Zeze.Builtin.HotDistribute.BVariable();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BVariable)_o_);
    }

    public void assign(BVariable _o_) {
        _Id = _o_.getId();
        _Name = _o_.getName();
        _Type = _o_.getType();
        _Key = _o_.getKey();
        _Value = _o_.getValue();
    }

    public void assign(BVariable.Data _o_) {
        _Id = _o_._Id;
        _Name = _o_._Name;
        _Type = _o_._Type;
        _Key = _o_._Key;
        _Value = _o_._Value;
    }

    @Override
    public BVariable.Data copy() {
        var _c_ = new BVariable.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BVariable.Data _a_, BVariable.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BVariable.Data clone() {
        return (BVariable.Data)super.clone();
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
        _s_.append("Zeze.Builtin.HotDistribute.BVariable: {\n");
        _s_.append(_i1_).append("Id=").append(_Id).append(",\n");
        _s_.append(_i1_).append("Name=").append(_Name).append(",\n");
        _s_.append(_i1_).append("Type=").append(_Type).append(",\n");
        _s_.append(_i1_).append("Key=").append(_Key).append(",\n");
        _s_.append(_i1_).append("Value=").append(_Value).append('\n');
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
            int _x_ = _Id;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = _Name;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = _Type;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = _Key;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = _Value;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Id = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Name = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _Type = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _Key = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _Value = _o_.ReadString(_t_);
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
        if (!(_o_ instanceof BVariable.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BVariable.Data)_o_;
        if (_Id != _b_._Id)
            return false;
        if (!_Name.equals(_b_._Name))
            return false;
        if (!_Type.equals(_b_._Type))
            return false;
        if (!_Key.equals(_b_._Key))
            return false;
        if (!_Value.equals(_b_._Value))
            return false;
        return true;
    }
}
}
