// auto-generated @formatter:off
package Zeze.Builtin.Collections.DepartmentTree;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BDepartmentRoot extends Zeze.Transaction.Bean implements BDepartmentRootReadOnly {
    public static final long TYPEID = 50884757418508709L;

    private String _Root; // 群主
    private final Zeze.Transaction.Collections.PMap2<String, Zeze.Transaction.DynamicBean> _Managers;

    public static Zeze.Transaction.DynamicBean newDynamicBean_Managers() {
        return new Zeze.Transaction.DynamicBean(2, Zeze.Collections.DepartmentTree::getSpecialTypeIdFromBean, Zeze.Collections.DepartmentTree::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_2(Zeze.Transaction.Bean _b_) {
        return Zeze.Collections.DepartmentTree.getSpecialTypeIdFromBean(_b_);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_2(long _t_) {
        return Zeze.Collections.DepartmentTree.createBeanFromSpecialTypeId(_t_);
    }

    private long _NextDepartmentId; // 部门Id种子
    private final Zeze.Transaction.Collections.PMap1<String, Long> _Childs; // name 2 id。采用整体保存，因为需要排序和重名判断。需要加数量上限。
    private final Zeze.Transaction.DynamicBean _Data;

    public static Zeze.Transaction.DynamicBean newDynamicBean_Data() {
        return new Zeze.Transaction.DynamicBean(5, Zeze.Collections.DepartmentTree::getSpecialTypeIdFromBean, Zeze.Collections.DepartmentTree::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_5(Zeze.Transaction.Bean _b_) {
        return Zeze.Collections.DepartmentTree.getSpecialTypeIdFromBean(_b_);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_5(long _t_) {
        return Zeze.Collections.DepartmentTree.createBeanFromSpecialTypeId(_t_);
    }

    @Override
    public String getRoot() {
        if (!isManaged())
            return _Root;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Root;
        var log = (Log__Root)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Root;
    }

    public void setRoot(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Root = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Root(this, 1, _v_));
    }

    public Zeze.Transaction.Collections.PMap2<String, Zeze.Transaction.DynamicBean> getManagers() {
        return _Managers;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getManagersReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Managers);
    }

    @Override
    public long getNextDepartmentId() {
        if (!isManaged())
            return _NextDepartmentId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _NextDepartmentId;
        var log = (Log__NextDepartmentId)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _NextDepartmentId;
    }

    public void setNextDepartmentId(long _v_) {
        if (!isManaged()) {
            _NextDepartmentId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__NextDepartmentId(this, 3, _v_));
    }

    public Zeze.Transaction.Collections.PMap1<String, Long> getChilds() {
        return _Childs;
    }

    @Override
    public Zeze.Transaction.Collections.PMap1ReadOnly<String, Long> getChildsReadOnly() {
        return new Zeze.Transaction.Collections.PMap1ReadOnly<>(_Childs);
    }

    public Zeze.Transaction.DynamicBean getData() {
        return _Data;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getDataReadOnly() {
        return _Data;
    }

    @SuppressWarnings("deprecation")
    public BDepartmentRoot() {
        _Root = "";
        _Managers = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Collections.DepartmentTree::getSpecialTypeIdFromBean, Zeze.Collections.DepartmentTree::createBeanFromSpecialTypeId);
        _Managers.variableId(2);
        _Childs = new Zeze.Transaction.Collections.PMap1<>(String.class, Long.class);
        _Childs.variableId(4);
        _Data = newDynamicBean_Data();
    }

    @SuppressWarnings("deprecation")
    public BDepartmentRoot(String _Root_, long _NextDepartmentId_) {
        if (_Root_ == null)
            _Root_ = "";
        _Root = _Root_;
        _Managers = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Collections.DepartmentTree::getSpecialTypeIdFromBean, Zeze.Collections.DepartmentTree::createBeanFromSpecialTypeId);
        _Managers.variableId(2);
        _NextDepartmentId = _NextDepartmentId_;
        _Childs = new Zeze.Transaction.Collections.PMap1<>(String.class, Long.class);
        _Childs.variableId(4);
        _Data = newDynamicBean_Data();
    }

    @Override
    public void reset() {
        setRoot("");
        _Managers.clear();
        setNextDepartmentId(0);
        _Childs.clear();
        _Data.reset();
        _unknown_ = null;
    }

    public void assign(BDepartmentRoot _o_) {
        setRoot(_o_.getRoot());
        _Managers.clear();
        for (var _e_ : _o_._Managers.entrySet())
            _Managers.put(_e_.getKey(), _e_.getValue().copy());
        setNextDepartmentId(_o_.getNextDepartmentId());
        _Childs.assign(_o_._Childs);
        _Data.assign(_o_._Data);
        _unknown_ = _o_._unknown_;
    }

    public BDepartmentRoot copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BDepartmentRoot copy() {
        var _c_ = new BDepartmentRoot();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BDepartmentRoot _a_, BDepartmentRoot _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Root extends Zeze.Transaction.Logs.LogString {
        public Log__Root(BDepartmentRoot _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BDepartmentRoot)getBelong())._Root = value; }
    }

    private static final class Log__NextDepartmentId extends Zeze.Transaction.Logs.LogLong {
        public Log__NextDepartmentId(BDepartmentRoot _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BDepartmentRoot)getBelong())._NextDepartmentId = value; }
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Collections.DepartmentTree.BDepartmentRoot: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Root=").append(getRoot()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Managers={");
        if (!_Managers.isEmpty()) {
            _s_.append(System.lineSeparator());
            _l_ += 4;
            for (var _e_ : _Managers.entrySet()) {
                _s_.append(Zeze.Util.Str.indent(_l_)).append("Key=").append(_e_.getKey()).append(',').append(System.lineSeparator());
                _s_.append(Zeze.Util.Str.indent(_l_)).append("Value=").append(System.lineSeparator());
                _e_.getValue().getBean().buildString(_s_, _l_ + 4);
                _s_.append(',').append(System.lineSeparator());
            }
            _l_ -= 4;
            _s_.append(Zeze.Util.Str.indent(_l_));
        }
        _s_.append('}').append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("NextDepartmentId=").append(getNextDepartmentId()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Childs={");
        if (!_Childs.isEmpty()) {
            _s_.append(System.lineSeparator());
            _l_ += 4;
            for (var _e_ : _Childs.entrySet()) {
                _s_.append(Zeze.Util.Str.indent(_l_)).append("Key=").append(_e_.getKey()).append(',').append(System.lineSeparator());
                _s_.append(Zeze.Util.Str.indent(_l_)).append("Value=").append(_e_.getValue()).append(',').append(System.lineSeparator());
            }
            _l_ -= 4;
            _s_.append(Zeze.Util.Str.indent(_l_));
        }
        _s_.append('}').append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Data=").append(System.lineSeparator());
        _Data.getBean().buildString(_s_, _l_ + 4);
        _s_.append(System.lineSeparator());
        _l_ -= 4;
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
            String _x_ = getRoot();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _Managers;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.DYNAMIC);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            long _x_ = getNextDepartmentId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Childs;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.INTEGER);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _o_.WriteLong(_e_.getValue());
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _Data;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
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
            setRoot(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Managers;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = new Zeze.Transaction.DynamicBean(0, Zeze.Collections.DepartmentTree::getSpecialTypeIdFromBean, Zeze.Collections.DepartmentTree::createBeanFromSpecialTypeId);
                    _o_.ReadDynamic(_v_, _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setNextDepartmentId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            var _x_ = _Childs;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadLong(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _o_.ReadDynamic(_Data, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BDepartmentRoot))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BDepartmentRoot)_o_;
        if (!getRoot().equals(_b_.getRoot()))
            return false;
        if (!_Managers.equals(_b_._Managers))
            return false;
        if (getNextDepartmentId() != _b_.getNextDepartmentId())
            return false;
        if (!_Childs.equals(_b_._Childs))
            return false;
        if (!_Data.equals(_b_._Data))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Managers.initRootInfo(_r_, this);
        _Childs.initRootInfo(_r_, this);
        _Data.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Managers.initRootInfoWithRedo(_r_, this);
        _Childs.initRootInfoWithRedo(_r_, this);
        _Data.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getNextDepartmentId() < 0)
            return true;
        for (var _v_ : _Childs.values()) {
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
                case 1: _Root = _v_.stringValue(); break;
                case 2: _Managers.followerApply(_v_); break;
                case 3: _NextDepartmentId = _v_.longValue(); break;
                case 4: _Childs.followerApply(_v_); break;
                case 5: _Data.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setRoot(_r_.getString(_pn_ + "Root"));
        if (getRoot() == null)
            setRoot("");
        Zeze.Serialize.Helper.decodeJsonMap(this, "Managers", _Managers, _r_.getString(_pn_ + "Managers"));
        setNextDepartmentId(_r_.getLong(_pn_ + "NextDepartmentId"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "Childs", _Childs, _r_.getString(_pn_ + "Childs"));
        Zeze.Serialize.Helper.decodeJsonDynamic(_Data, _r_.getString(_pn_ + "Data"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Root", getRoot());
        _s_.appendString(_pn_ + "Managers", Zeze.Serialize.Helper.encodeJson(_Managers));
        _s_.appendLong(_pn_ + "NextDepartmentId", getNextDepartmentId());
        _s_.appendString(_pn_ + "Childs", Zeze.Serialize.Helper.encodeJson(_Childs));
        _s_.appendString(_pn_ + "Data", Zeze.Serialize.Helper.encodeJson(_Data));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Root", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Managers", "map", "string", "dynamic"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "NextDepartmentId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Childs", "map", "string", "long"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "Data", "dynamic", "", ""));
        return _v_;
    }
}
