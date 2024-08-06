// auto-generated rocks @formatter:off
package Zeze.Builtin.TestRocks;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BValue extends Zeze.Raft.RocksRaft.Bean {
    public static final long TYPEID = -7620770686653123067L;

    private int _Int;
    private boolean _Bool;
    private float _Float;
    private double _double;
    private String _String;
    private Zeze.Net.Binary _Binary;
    private final Zeze.Raft.RocksRaft.CollSet1<Integer> _SetInt;
    private final Zeze.Raft.RocksRaft.CollSet1<Zeze.Builtin.TestRocks.BeanKey> _SetBeankey;
    private final Zeze.Raft.RocksRaft.CollMap1<Integer, Integer> _MapInt;
    private final Zeze.Raft.RocksRaft.CollMap2<Integer, Zeze.Builtin.TestRocks.BValue> _MapBean;
    private Zeze.Builtin.TestRocks.BeanKey _Beankey;

    private transient Object __zeze_map_key__;

    @Override
    public Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object value) {
        __zeze_map_key__ = value;
    }

    public int getInt() {
        if (!isManaged())
            return _Int;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _Int;
        var log = txn.getLog(objectId() + 1);
        if (log == null)
            return _Int;
        return ((Zeze.Raft.RocksRaft.Log1.LogInt)log).value;
    }

    public void setInt(int value) {
        if (!isManaged()) {
            _Int = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogInt(this, 1, value));
    }

    public boolean isBool() {
        if (!isManaged())
            return _Bool;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _Bool;
        var log = txn.getLog(objectId() + 2);
        if (log == null)
            return _Bool;
        return ((Zeze.Raft.RocksRaft.Log1.LogBool)log).value;
    }

    public void setBool(boolean value) {
        if (!isManaged()) {
            _Bool = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogBool(this, 2, value));
    }

    public float getFloat() {
        if (!isManaged())
            return _Float;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _Float;
        var log = txn.getLog(objectId() + 3);
        if (log == null)
            return _Float;
        return ((Zeze.Raft.RocksRaft.Log1.LogFloat)log).value;
    }

    public void setFloat(float value) {
        if (!isManaged()) {
            _Float = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogFloat(this, 3, value));
    }

    public double getDouble() {
        if (!isManaged())
            return _double;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _double;
        var log = txn.getLog(objectId() + 4);
        if (log == null)
            return _double;
        return ((Zeze.Raft.RocksRaft.Log1.LogDouble)log).value;
    }

    public void setDouble(double value) {
        if (!isManaged()) {
            _double = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogDouble(this, 4, value));
    }

    public String getString() {
        if (!isManaged())
            return _String;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _String;
        var log = txn.getLog(objectId() + 5);
        if (log == null)
            return _String;
        return ((Zeze.Raft.RocksRaft.Log1.LogString)log).value;
    }

    public void setString(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _String = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogString(this, 5, value));
    }

    public Zeze.Net.Binary getBinary() {
        if (!isManaged())
            return _Binary;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _Binary;
        var log = txn.getLog(objectId() + 6);
        if (log == null)
            return _Binary;
        return ((Zeze.Raft.RocksRaft.Log1.LogBinary)log).value;
    }

    public void setBinary(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Binary = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogBinary(this, 6, value));
    }

    public Zeze.Raft.RocksRaft.CollSet1<Integer> getSetInt() {
        return _SetInt;
    }

    public Zeze.Raft.RocksRaft.CollSet1<Zeze.Builtin.TestRocks.BeanKey> getSetBeankey() {
        return _SetBeankey;
    }

    public Zeze.Raft.RocksRaft.CollMap1<Integer, Integer> getMapInt() {
        return _MapInt;
    }

    public Zeze.Raft.RocksRaft.CollMap2<Integer, Zeze.Builtin.TestRocks.BValue> getMapBean() {
        return _MapBean;
    }

    @SuppressWarnings("unchecked")
    public Zeze.Builtin.TestRocks.BeanKey getBeankey() {
        if (!isManaged())
            return _Beankey;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _Beankey;
        var log = txn.getLog(objectId() + 11);
        if (null == log)
            return _Beankey;
        return ((Zeze.Raft.RocksRaft.Log1.LogBeanKey<Zeze.Builtin.TestRocks.BeanKey>)log).value;
    }

    public void setBeankey(Zeze.Builtin.TestRocks.BeanKey value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Beankey = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogBeanKey<>(Zeze.Builtin.TestRocks.BeanKey.class, this, 11, value));
    }

    public BValue() {
        _String = "";
        _Binary = Zeze.Net.Binary.Empty;
        _SetInt = new Zeze.Raft.RocksRaft.CollSet1<>(Integer.class);
        _SetInt.variableId(7);
        _SetBeankey = new Zeze.Raft.RocksRaft.CollSet1<>(Zeze.Builtin.TestRocks.BeanKey.class);
        _SetBeankey.variableId(8);
        _MapInt = new Zeze.Raft.RocksRaft.CollMap1<>(Integer.class, Integer.class);
        _MapInt.variableId(9);
        _MapBean = new Zeze.Raft.RocksRaft.CollMap2<>(Integer.class, Zeze.Builtin.TestRocks.BValue.class);
        _MapBean.variableId(10);
        _Beankey = new Zeze.Builtin.TestRocks.BeanKey();
    }

    public BValue(int _Int_, boolean _Bool_, float _Float_, double _double_, String _String_, Zeze.Net.Binary _Binary_, Zeze.Builtin.TestRocks.BeanKey _Beankey_) {
        _Int = _Int_;
        _Bool = _Bool_;
        _Float = _Float_;
        _double = _double_;
        if (_String_ == null)
            throw new IllegalArgumentException();
        _String = _String_;
        if (_Binary_ == null)
            throw new IllegalArgumentException();
        _Binary = _Binary_;
        _SetInt = new Zeze.Raft.RocksRaft.CollSet1<>(Integer.class);
        _SetInt.variableId(7);
        _SetBeankey = new Zeze.Raft.RocksRaft.CollSet1<>(Zeze.Builtin.TestRocks.BeanKey.class);
        _SetBeankey.variableId(8);
        _MapInt = new Zeze.Raft.RocksRaft.CollMap1<>(Integer.class, Integer.class);
        _MapInt.variableId(9);
        _MapBean = new Zeze.Raft.RocksRaft.CollMap2<>(Integer.class, Zeze.Builtin.TestRocks.BValue.class);
        _MapBean.variableId(10);
        if (_Beankey_ == null)
            throw new IllegalArgumentException();
        _Beankey = _Beankey_;
    }

    public void assign(BValue other) {
        setInt(other.getInt());
        setBool(other.isBool());
        setFloat(other.getFloat());
        setDouble(other.getDouble());
        setString(other.getString());
        setBinary(other.getBinary());
        _SetInt.clear();
        for (var e : other._SetInt)
            _SetInt.add(e);
        _SetBeankey.clear();
        for (var e : other._SetBeankey)
            _SetBeankey.add(e);
        _MapInt.clear();
        for (var e : other._MapInt.entrySet())
            _MapInt.put(e.getKey(), e.getValue());
        _MapBean.clear();
        for (var e : other._MapBean.entrySet())
            _MapBean.put(e.getKey(), e.getValue());
        setBeankey(other.getBeankey());
    }

    public BValue copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BValue copy() {
        var copy = new BValue();
        copy.assign(this);
        return copy;
    }

    public static void swap(BValue a, BValue b) {
        BValue save = a.copy();
        a.assign(b);
        b.assign(save);
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
        _s_.append("Zeze.Builtin.TestRocks.BValue: {\n");
        _s_.append(_i1_).append("Int=").append(getInt()).append(",\n");
        _s_.append(_i1_).append("Bool=").append(isBool()).append(",\n");
        _s_.append(_i1_).append("Float=").append(getFloat()).append(",\n");
        _s_.append(_i1_).append("double=").append(getDouble()).append(",\n");
        _s_.append(_i1_).append("String=").append(getString()).append(",\n");
        _s_.append(_i1_).append("Binary=").append(getBinary()).append(",\n");
        _s_.append(_i1_).append("SetInt=[\n");
        for (var _item_ : getSetInt()) {
            _s_.append(_i2_).append("Item=").append(_item_).append(",\n");
        }
        _s_.append(_i1_).append("],\n");
        _s_.append(_i1_).append("SetBeankey=[\n");
        for (var _item_ : getSetBeankey()) {
            _s_.append(_i2_).append("Item=");
            _item_.buildString(_s_, _l_ + 12);
            _s_.append(",\n");
        }
        _s_.append(_i1_).append("],\n");
        _s_.append(_i1_).append("MapInt=[\n");
        for (var _kv_ : getMapInt().entrySet()) {
            _s_.append(_i2_).append("Key=").append(_kv_.getKey()).append(",\n");
            _s_.append(_i2_).append("Value=").append(_kv_.getValue()).append(",\n");
        }
        _s_.append(_i1_).append("],\n");
        _s_.append(_i1_).append("MapBean=[\n");
        for (var _kv_ : getMapBean().entrySet()) {
            _s_.append(_i2_).append("Key=").append(_kv_.getKey()).append(",\n");
            _s_.append(_i2_).append("Value=");
            _kv_.getValue().buildString(_s_, _l_ + 12);
            _s_.append(",\n");
        }
        _s_.append(_i1_).append("],\n");
        _s_.append(_i1_).append("Beankey=");
        getBeankey().buildString(_s_, _l_ + 8);
        _s_.append('\n');
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
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

    @Override
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            int _x_ = getInt();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            boolean _x_ = isBool();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            float _x_ = getFloat();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.FLOAT);
                _o_.WriteFloat(_x_);
            }
        }
        {
            double _x_ = getDouble();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.DOUBLE);
                _o_.WriteDouble(_x_);
            }
        }
        {
            String _x_ = getString();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getBinary();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = getSetInt();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.LIST);
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
            var _x_ = getSetBeankey();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (var _v_ : _x_) {
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = getMapInt();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 9, ByteBuffer.MAP);
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
        {
            var _x_ = getMapBean();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 10, ByteBuffer.MAP);
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 11, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getBeankey().encode(_o_);
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
            _Int = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Bool = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _Float = _o_.ReadFloat(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _double = _o_.ReadDouble(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _String = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            _Binary = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            var _x_ = getSetInt();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 8) {
            var _x_ = getSetBeankey();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.TestRocks.BeanKey(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 9) {
            var _x_ = getMapInt();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadInt(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 10) {
            var _x_ = getMapBean();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.TestRocks.BValue(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 11) {
            _o_.ReadBean(_Beankey, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Raft.RocksRaft.Record.RootInfo root) {
        _SetInt.initRootInfo(root, this);
        _SetBeankey.initRootInfo(root, this);
        _MapInt.initRootInfo(root, this);
        _MapBean.initRootInfo(root, this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void leaderApplyNoRecursive(Zeze.Raft.RocksRaft.Log vlog) {
        switch (vlog.getVariableId()) {
            case 1: _Int = ((Zeze.Raft.RocksRaft.Log1.LogInt)vlog).value; break;
            case 2: _Bool = ((Zeze.Raft.RocksRaft.Log1.LogBool)vlog).value; break;
            case 3: _Float = ((Zeze.Raft.RocksRaft.Log1.LogFloat)vlog).value; break;
            case 4: _double = ((Zeze.Raft.RocksRaft.Log1.LogDouble)vlog).value; break;
            case 5: _String = ((Zeze.Raft.RocksRaft.Log1.LogString)vlog).value; break;
            case 6: _Binary = ((Zeze.Raft.RocksRaft.Log1.LogBinary)vlog).value; break;
            case 7: _SetInt.leaderApplyNoRecursive(vlog); break;
            case 8: _SetBeankey.leaderApplyNoRecursive(vlog); break;
            case 9: _MapInt.leaderApplyNoRecursive(vlog); break;
            case 10: _MapBean.leaderApplyNoRecursive(vlog); break;
            case 11: _Beankey = ((Zeze.Raft.RocksRaft.Log1.LogBeanKey<Zeze.Builtin.TestRocks.BeanKey>)vlog).value; break;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void followerApply(Zeze.Raft.RocksRaft.Log log) {
        var vars = ((Zeze.Raft.RocksRaft.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _Int = ((Zeze.Raft.RocksRaft.Log1.LogInt)vlog).value; break;
                case 2: _Bool = ((Zeze.Raft.RocksRaft.Log1.LogBool)vlog).value; break;
                case 3: _Float = ((Zeze.Raft.RocksRaft.Log1.LogFloat)vlog).value; break;
                case 4: _double = ((Zeze.Raft.RocksRaft.Log1.LogDouble)vlog).value; break;
                case 5: _String = ((Zeze.Raft.RocksRaft.Log1.LogString)vlog).value; break;
                case 6: _Binary = ((Zeze.Raft.RocksRaft.Log1.LogBinary)vlog).value; break;
                case 7: _SetInt.followerApply(vlog); break;
                case 8: _SetBeankey.followerApply(vlog); break;
                case 9: _MapInt.followerApply(vlog); break;
                case 10: _MapBean.followerApply(vlog); break;
                case 11: _Beankey = ((Zeze.Raft.RocksRaft.Log1.LogBeanKey<Zeze.Builtin.TestRocks.BeanKey>)vlog).value; break;
            }
        }
    }
}
