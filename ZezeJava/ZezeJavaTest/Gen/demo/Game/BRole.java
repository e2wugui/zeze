// auto-generated @formatter:off
package demo.Game;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BRole extends Zeze.Transaction.Bean implements BRoleReadOnly {
    public static final long TYPEID = 6807186162468233202L;

    private int _Level;
    private long _Exp;
    private long _Money;
    private int _Hp;
    private long _DamageTotal;
    private final Zeze.Transaction.Collections.PMap2<Integer, demo.Game.BItem> _Bag;
    private static final Zeze.Transaction.Collections.Map2Meta<Integer, demo.Game.BItem> meta2_Bag
            = Zeze.Transaction.Collections.Map2Meta.get(Integer.class, demo.Game.BItem.class);

    private final Zeze.Transaction.Collections.PMap2<Integer, demo.Game.BItem> _Equip;
    private static final Zeze.Transaction.Collections.Map2Meta<Integer, demo.Game.BItem> meta2_Equip
            = Zeze.Transaction.Collections.Map2Meta.get(Integer.class, demo.Game.BItem.class);

    private final Zeze.Transaction.Collections.PMap1<Long, Integer> _Friends;
    private static final Zeze.Transaction.Collections.Map1Meta<Long, Integer> meta2_Friends
            = Zeze.Transaction.Collections.Map1Meta.get(Long.class, Integer.class);

    private final Zeze.Transaction.Collections.PList1<Long> _Tracks;
    private static final Zeze.Transaction.Collections.List1Meta<Long> meta1_Tracks
            = Zeze.Transaction.Collections.List1Meta.get(Long.class);

    private final Zeze.Transaction.Collections.PList1<Long> _QuestLog;
    private static final Zeze.Transaction.Collections.List1Meta<Long> meta1_QuestLog
            = Zeze.Transaction.Collections.List1Meta.get(Long.class);

    private final Zeze.Transaction.Collections.PList1<Long> _GiftLog;
    private static final Zeze.Transaction.Collections.List1Meta<Long> meta1_GiftLog
            = Zeze.Transaction.Collections.List1Meta.get(Long.class);

    private final Zeze.Transaction.Collections.PList2<demo.Game.BItem> _Warehouse;
    private static final Zeze.Transaction.Collections.List2Meta<demo.Game.BItem> meta1_Warehouse
            = Zeze.Transaction.Collections.List2Meta.get(demo.Game.BItem.class);

    private static final java.lang.invoke.VarHandle vh_Level;
    private static final java.lang.invoke.VarHandle vh_Exp;
    private static final java.lang.invoke.VarHandle vh_Money;
    private static final java.lang.invoke.VarHandle vh_Hp;
    private static final java.lang.invoke.VarHandle vh_DamageTotal;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Level = _l_.findVarHandle(BRole.class, "_Level", int.class);
            vh_Exp = _l_.findVarHandle(BRole.class, "_Exp", long.class);
            vh_Money = _l_.findVarHandle(BRole.class, "_Money", long.class);
            vh_Hp = _l_.findVarHandle(BRole.class, "_Hp", int.class);
            vh_DamageTotal = _l_.findVarHandle(BRole.class, "_DamageTotal", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getLevel() {
        if (!isManaged())
            return _Level;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Level;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Level;
    }

    public void setLevel(int _v_) {
        if (!isManaged()) {
            _Level = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_Level, _v_));
    }

    @Override
    public long getExp() {
        if (!isManaged())
            return _Exp;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Exp;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Exp;
    }

    public void setExp(long _v_) {
        if (!isManaged()) {
            _Exp = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_Exp, _v_));
    }

    @Override
    public long getMoney() {
        if (!isManaged())
            return _Money;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Money;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _Money;
    }

    public void setMoney(long _v_) {
        if (!isManaged()) {
            _Money = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 3, vh_Money, _v_));
    }

    @Override
    public int getHp() {
        if (!isManaged())
            return _Hp;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Hp;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _Hp;
    }

    public void setHp(int _v_) {
        if (!isManaged()) {
            _Hp = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 4, vh_Hp, _v_));
    }

    @Override
    public long getDamageTotal() {
        if (!isManaged())
            return _DamageTotal;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _DamageTotal;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _DamageTotal;
    }

    public void setDamageTotal(long _v_) {
        if (!isManaged()) {
            _DamageTotal = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 5, vh_DamageTotal, _v_));
    }

    public Zeze.Transaction.Collections.PMap2<Integer, demo.Game.BItem> getBag() {
        return _Bag;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Integer, demo.Game.BItem, demo.Game.BItemReadOnly> getBagReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Bag);
    }

    public Zeze.Transaction.Collections.PMap2<Integer, demo.Game.BItem> getEquip() {
        return _Equip;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Integer, demo.Game.BItem, demo.Game.BItemReadOnly> getEquipReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Equip);
    }

    public Zeze.Transaction.Collections.PMap1<Long, Integer> getFriends() {
        return _Friends;
    }

    @Override
    public Zeze.Transaction.Collections.PMap1ReadOnly<Long, Integer> getFriendsReadOnly() {
        return new Zeze.Transaction.Collections.PMap1ReadOnly<>(_Friends);
    }

    public Zeze.Transaction.Collections.PList1<Long> getTracks() {
        return _Tracks;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getTracksReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_Tracks);
    }

    public Zeze.Transaction.Collections.PList1<Long> getQuestLog() {
        return _QuestLog;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getQuestLogReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_QuestLog);
    }

    public Zeze.Transaction.Collections.PList1<Long> getGiftLog() {
        return _GiftLog;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getGiftLogReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_GiftLog);
    }

    public Zeze.Transaction.Collections.PList2<demo.Game.BItem> getWarehouse() {
        return _Warehouse;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<demo.Game.BItem, demo.Game.BItemReadOnly> getWarehouseReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_Warehouse);
    }

    @SuppressWarnings("deprecation")
    public BRole() {
        _Bag = new Zeze.Transaction.Collections.PMap2<>(meta2_Bag);
        _Bag.variableId(6);
        _Equip = new Zeze.Transaction.Collections.PMap2<>(meta2_Equip);
        _Equip.variableId(7);
        _Friends = new Zeze.Transaction.Collections.PMap1<>(meta2_Friends);
        _Friends.variableId(8);
        _Tracks = new Zeze.Transaction.Collections.PList1<>(meta1_Tracks);
        _Tracks.variableId(9);
        _QuestLog = new Zeze.Transaction.Collections.PList1<>(meta1_QuestLog);
        _QuestLog.variableId(10);
        _GiftLog = new Zeze.Transaction.Collections.PList1<>(meta1_GiftLog);
        _GiftLog.variableId(11);
        _Warehouse = new Zeze.Transaction.Collections.PList2<>(meta1_Warehouse);
        _Warehouse.variableId(12);
    }

    @SuppressWarnings("deprecation")
    public BRole(int _Level_, long _Exp_, long _Money_, int _Hp_, long _DamageTotal_) {
        _Level = _Level_;
        _Exp = _Exp_;
        _Money = _Money_;
        _Hp = _Hp_;
        _DamageTotal = _DamageTotal_;
        _Bag = new Zeze.Transaction.Collections.PMap2<>(meta2_Bag);
        _Bag.variableId(6);
        _Equip = new Zeze.Transaction.Collections.PMap2<>(meta2_Equip);
        _Equip.variableId(7);
        _Friends = new Zeze.Transaction.Collections.PMap1<>(meta2_Friends);
        _Friends.variableId(8);
        _Tracks = new Zeze.Transaction.Collections.PList1<>(meta1_Tracks);
        _Tracks.variableId(9);
        _QuestLog = new Zeze.Transaction.Collections.PList1<>(meta1_QuestLog);
        _QuestLog.variableId(10);
        _GiftLog = new Zeze.Transaction.Collections.PList1<>(meta1_GiftLog);
        _GiftLog.variableId(11);
        _Warehouse = new Zeze.Transaction.Collections.PList2<>(meta1_Warehouse);
        _Warehouse.variableId(12);
    }

    @Override
    public void reset() {
        setLevel(0);
        setExp(0);
        setMoney(0);
        setHp(0);
        setDamageTotal(0);
        _Bag.clear();
        _Equip.clear();
        _Friends.clear();
        _Tracks.clear();
        _QuestLog.clear();
        _GiftLog.clear();
        _Warehouse.clear();
        _unknown_ = null;
    }

    public void assign(BRole _o_) {
        setLevel(_o_.getLevel());
        setExp(_o_.getExp());
        setMoney(_o_.getMoney());
        setHp(_o_.getHp());
        setDamageTotal(_o_.getDamageTotal());
        _Bag.clear();
        for (var _e_ : _o_._Bag.entrySet())
            _Bag.put(_e_.getKey(), _e_.getValue().copy());
        _Equip.clear();
        for (var _e_ : _o_._Equip.entrySet())
            _Equip.put(_e_.getKey(), _e_.getValue().copy());
        _Friends.assign(_o_._Friends);
        _Tracks.assign(_o_._Tracks);
        _QuestLog.assign(_o_._QuestLog);
        _GiftLog.assign(_o_._GiftLog);
        _Warehouse.clear();
        for (var _e_ : _o_._Warehouse)
            _Warehouse.add(_e_.copy());
        _unknown_ = _o_._unknown_;
    }

    public BRole copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BRole copy() {
        var _c_ = new BRole();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BRole _a_, BRole _b_) {
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
        _s_.append("demo.Game.BRole: {\n");
        _s_.append(_i1_).append("Level=").append(getLevel()).append(",\n");
        _s_.append(_i1_).append("Exp=").append(getExp()).append(",\n");
        _s_.append(_i1_).append("Money=").append(getMoney()).append(",\n");
        _s_.append(_i1_).append("Hp=").append(getHp()).append(",\n");
        _s_.append(_i1_).append("DamageTotal=").append(getDamageTotal()).append(",\n");
        _s_.append(_i1_).append("Bag={");
        if (!_Bag.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _Bag.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Bag.size()).append("]\n");
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
        _s_.append(_i1_).append("Equip={");
        if (!_Equip.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _Equip.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Equip.size()).append("]\n");
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
        _s_.append(_i1_).append("Friends={");
        if (!_Friends.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _Friends.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Friends.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=").append(_e_.getValue()).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("Tracks=[");
        if (!_Tracks.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _Tracks) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Tracks.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("QuestLog=[");
        if (!_QuestLog.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _QuestLog) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_QuestLog.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("GiftLog=[");
        if (!_GiftLog.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _GiftLog) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_GiftLog.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("Warehouse=[");
        if (!_Warehouse.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _Warehouse) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Warehouse.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=");
                _v_.buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("]\n");
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
            int _x_ = getLevel();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getExp();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getMoney();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getHp();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getDamageTotal();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Bag;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.MAP);
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
            var _x_ = _Equip;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.MAP);
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
            var _x_ = _Friends;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.MAP);
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
            var _x_ = _Tracks;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 9, ByteBuffer.LIST);
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
            var _x_ = _QuestLog;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 10, ByteBuffer.LIST);
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
            var _x_ = _GiftLog;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 11, ByteBuffer.LIST);
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
            var _x_ = _Warehouse;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 12, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (var _v_ : _x_) {
                    _v_.encode(_o_);
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
            setLevel(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setExp(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setMoney(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setHp(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setDamageTotal(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            var _x_ = _Bag;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(new demo.Game.BItem(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            var _x_ = _Equip;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(new demo.Game.BItem(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 8) {
            var _x_ = _Friends;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadInt(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 9) {
            var _x_ = _Tracks;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 10) {
            var _x_ = _QuestLog;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 11) {
            var _x_ = _GiftLog;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 12) {
            var _x_ = _Warehouse;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new demo.Game.BItem(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BRole))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BRole)_o_;
        if (getLevel() != _b_.getLevel())
            return false;
        if (getExp() != _b_.getExp())
            return false;
        if (getMoney() != _b_.getMoney())
            return false;
        if (getHp() != _b_.getHp())
            return false;
        if (getDamageTotal() != _b_.getDamageTotal())
            return false;
        if (!_Bag.equals(_b_._Bag))
            return false;
        if (!_Equip.equals(_b_._Equip))
            return false;
        if (!_Friends.equals(_b_._Friends))
            return false;
        if (!_Tracks.equals(_b_._Tracks))
            return false;
        if (!_QuestLog.equals(_b_._QuestLog))
            return false;
        if (!_GiftLog.equals(_b_._GiftLog))
            return false;
        if (!_Warehouse.equals(_b_._Warehouse))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Bag.initRootInfo(_r_, this);
        _Equip.initRootInfo(_r_, this);
        _Friends.initRootInfo(_r_, this);
        _Tracks.initRootInfo(_r_, this);
        _QuestLog.initRootInfo(_r_, this);
        _GiftLog.initRootInfo(_r_, this);
        _Warehouse.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Bag.initRootInfoWithRedo(_r_, this);
        _Equip.initRootInfoWithRedo(_r_, this);
        _Friends.initRootInfoWithRedo(_r_, this);
        _Tracks.initRootInfoWithRedo(_r_, this);
        _QuestLog.initRootInfoWithRedo(_r_, this);
        _GiftLog.initRootInfoWithRedo(_r_, this);
        _Warehouse.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getLevel() < 0)
            return true;
        if (getExp() < 0)
            return true;
        if (getMoney() < 0)
            return true;
        if (getHp() < 0)
            return true;
        if (getDamageTotal() < 0)
            return true;
        for (var _v_ : _Bag.values()) {
            if (_v_.negativeCheck())
                return true;
        }
        for (var _v_ : _Equip.values()) {
            if (_v_.negativeCheck())
                return true;
        }
        for (var _v_ : _Friends.values()) {
            if (_v_ < 0)
                return true;
        }
        for (var _v_ : _Tracks) {
            if (_v_ < 0)
                return true;
        }
        for (var _v_ : _QuestLog) {
            if (_v_ < 0)
                return true;
        }
        for (var _v_ : _GiftLog) {
            if (_v_ < 0)
                return true;
        }
        for (var _v_ : _Warehouse) {
            if (_v_.negativeCheck())
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
                case 1: _Level = _v_.intValue(); break;
                case 2: _Exp = _v_.longValue(); break;
                case 3: _Money = _v_.longValue(); break;
                case 4: _Hp = _v_.intValue(); break;
                case 5: _DamageTotal = _v_.longValue(); break;
                case 6: _Bag.followerApply(_v_); break;
                case 7: _Equip.followerApply(_v_); break;
                case 8: _Friends.followerApply(_v_); break;
                case 9: _Tracks.followerApply(_v_); break;
                case 10: _QuestLog.followerApply(_v_); break;
                case 11: _GiftLog.followerApply(_v_); break;
                case 12: _Warehouse.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setLevel(_r_.getInt(_pn_ + "Level"));
        setExp(_r_.getLong(_pn_ + "Exp"));
        setMoney(_r_.getLong(_pn_ + "Money"));
        setHp(_r_.getInt(_pn_ + "Hp"));
        setDamageTotal(_r_.getLong(_pn_ + "DamageTotal"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "Bag", _Bag, _r_.getString(_pn_ + "Bag"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "Equip", _Equip, _r_.getString(_pn_ + "Equip"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "Friends", _Friends, _r_.getString(_pn_ + "Friends"));
        Zeze.Serialize.Helper.decodeJsonList(_Tracks, Long.class, _r_.getString(_pn_ + "Tracks"));
        Zeze.Serialize.Helper.decodeJsonList(_QuestLog, Long.class, _r_.getString(_pn_ + "QuestLog"));
        Zeze.Serialize.Helper.decodeJsonList(_GiftLog, Long.class, _r_.getString(_pn_ + "GiftLog"));
        Zeze.Serialize.Helper.decodeJsonList(_Warehouse, demo.Game.BItem.class, _r_.getString(_pn_ + "Warehouse"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "Level", getLevel());
        _s_.appendLong(_pn_ + "Exp", getExp());
        _s_.appendLong(_pn_ + "Money", getMoney());
        _s_.appendInt(_pn_ + "Hp", getHp());
        _s_.appendLong(_pn_ + "DamageTotal", getDamageTotal());
        _s_.appendString(_pn_ + "Bag", Zeze.Serialize.Helper.encodeJson(_Bag));
        _s_.appendString(_pn_ + "Equip", Zeze.Serialize.Helper.encodeJson(_Equip));
        _s_.appendString(_pn_ + "Friends", Zeze.Serialize.Helper.encodeJson(_Friends));
        _s_.appendString(_pn_ + "Tracks", Zeze.Serialize.Helper.encodeJson(_Tracks));
        _s_.appendString(_pn_ + "QuestLog", Zeze.Serialize.Helper.encodeJson(_QuestLog));
        _s_.appendString(_pn_ + "GiftLog", Zeze.Serialize.Helper.encodeJson(_GiftLog));
        _s_.appendString(_pn_ + "Warehouse", Zeze.Serialize.Helper.encodeJson(_Warehouse));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Level", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Exp", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Money", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Hp", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "DamageTotal", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "Bag", "map", "int", "demo.Game.BItem"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "Equip", "map", "int", "demo.Game.BItem"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(8, "Friends", "map", "long", "int"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(9, "Tracks", "list", "", "long"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(10, "QuestLog", "list", "", "long"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(11, "GiftLog", "list", "", "long"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(12, "Warehouse", "list", "", "demo.Game.BItem"));
        return _v_;
    }
}
