// auto-generated @formatter:off
package Game.Equip;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BChangedResult extends Zeze.Transaction.Bean implements BChangedResultReadOnly {
    public static final long TYPEID = -4276476463499273616L;

    public static final int ChangeTagNormalChanged = 0; // 普通增量修改。
    public static final int ChangeTagRecordIsRemoved = 1; // 整个记录删除了。
    public static final int ChangeTagRecordChanged = 2; // 整个记录发生了变更，需要先清除本地数据，再替换进去。

    private final Zeze.Transaction.Collections.PMap2<Integer, Game.Equip.BItem> _ItemsReplace; // key is position
    private static final Zeze.Transaction.Collections.Map2Meta<Integer, Game.Equip.BItem> meta2_ItemsReplace
            = Zeze.Transaction.Collections.Map2Meta.get(Integer.class, Game.Equip.BItem.class);

    private final Zeze.Transaction.Collections.PSet1<Integer> _ItemsRemove; // key is position
    private static final Zeze.Transaction.Collections.Set1Meta<Integer> meta1_ItemsRemove
            = Zeze.Transaction.Collections.Set1Meta.get(Integer.class);

    private int _ChangeTag;

    private static final java.lang.invoke.VarHandle vh_ChangeTag;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_ChangeTag = _l_.findVarHandle(BChangedResult.class, "_ChangeTag", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    public Zeze.Transaction.Collections.PMap2<Integer, Game.Equip.BItem> getItemsReplace() {
        return _ItemsReplace;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Game.Equip.BItem, Game.Equip.BItemReadOnly> getItemsReplaceReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_ItemsReplace);
    }

    public Zeze.Transaction.Collections.PSet1<Integer> getItemsRemove() {
        return _ItemsRemove;
    }

    @Override
    public Zeze.Transaction.Collections.PSet1ReadOnly<Integer> getItemsRemoveReadOnly() {
        return new Zeze.Transaction.Collections.PSet1ReadOnly<>(_ItemsRemove);
    }

    @Override
    public int getChangeTag() {
        if (!isManaged())
            return _ChangeTag;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ChangeTag;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _ChangeTag;
    }

    public void setChangeTag(int _v_) {
        if (!isManaged()) {
            _ChangeTag = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 3, vh_ChangeTag, _v_));
    }

    @SuppressWarnings("deprecation")
    public BChangedResult() {
        _ItemsReplace = new Zeze.Transaction.Collections.PMap2<>(meta2_ItemsReplace);
        _ItemsReplace.variableId(1);
        _ItemsRemove = new Zeze.Transaction.Collections.PSet1<>(meta1_ItemsRemove);
        _ItemsRemove.variableId(2);
    }

    @SuppressWarnings("deprecation")
    public BChangedResult(int _ChangeTag_) {
        _ItemsReplace = new Zeze.Transaction.Collections.PMap2<>(meta2_ItemsReplace);
        _ItemsReplace.variableId(1);
        _ItemsRemove = new Zeze.Transaction.Collections.PSet1<>(meta1_ItemsRemove);
        _ItemsRemove.variableId(2);
        _ChangeTag = _ChangeTag_;
    }

    @Override
    public void reset() {
        _ItemsReplace.clear();
        _ItemsRemove.clear();
        setChangeTag(0);
        _unknown_ = null;
    }

    public void assign(BChangedResult _o_) {
        _ItemsReplace.clear();
        for (var _e_ : _o_._ItemsReplace.entrySet())
            _ItemsReplace.put(_e_.getKey(), _e_.getValue().copy());
        _ItemsRemove.assign(_o_._ItemsRemove);
        setChangeTag(_o_.getChangeTag());
        _unknown_ = _o_._unknown_;
    }

    public BChangedResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BChangedResult copy() {
        var _c_ = new BChangedResult();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BChangedResult _a_, BChangedResult _b_) {
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
        _s_.append("Game.Equip.BChangedResult: {\n");
        _s_.append(_i1_).append("ItemsReplace={");
        if (!_ItemsReplace.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _ItemsReplace.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_ItemsReplace.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("ItemsRemove={");
        if (!_ItemsRemove.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _ItemsRemove) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_ItemsRemove.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("ChangeTag=").append(getChangeTag()).append('\n');
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
            var _x_ = _ItemsReplace;
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
            var _x_ = _ItemsRemove;
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
            int _x_ = getChangeTag();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
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
            var _x_ = _ItemsReplace;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(new Game.Equip.BItem(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _ItemsRemove;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setChangeTag(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BChangedResult))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BChangedResult)_o_;
        if (!_ItemsReplace.equals(_b_._ItemsReplace))
            return false;
        if (!_ItemsRemove.equals(_b_._ItemsRemove))
            return false;
        if (getChangeTag() != _b_.getChangeTag())
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _ItemsReplace.initRootInfo(_r_, this);
        _ItemsRemove.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _ItemsReplace.initRootInfoWithRedo(_r_, this);
        _ItemsRemove.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _ItemsReplace.values()) {
            if (_v_.negativeCheck())
                return true;
        }
        for (var _v_ : _ItemsRemove) {
            if (_v_ < 0)
                return true;
        }
        if (getChangeTag() < 0)
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
                case 1: _ItemsReplace.followerApply(_v_); break;
                case 2: _ItemsRemove.followerApply(_v_); break;
                case 3: _ChangeTag = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        Zeze.Serialize.Helper.decodeJsonMap(this, "ItemsReplace", _ItemsReplace, _r_.getString(_pn_ + "ItemsReplace"));
        Zeze.Serialize.Helper.decodeJsonSet(_ItemsRemove, Integer.class, _r_.getString(_pn_ + "ItemsRemove"));
        setChangeTag(_r_.getInt(_pn_ + "ChangeTag"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "ItemsReplace", Zeze.Serialize.Helper.encodeJson(_ItemsReplace));
        _s_.appendString(_pn_ + "ItemsRemove", Zeze.Serialize.Helper.encodeJson(_ItemsRemove));
        _s_.appendInt(_pn_ + "ChangeTag", getChangeTag());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ItemsReplace", "map", "int", "Game.Equip.BItem"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ItemsRemove", "set", "", "int"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "ChangeTag", "int", "", ""));
        return _v_;
    }
}
