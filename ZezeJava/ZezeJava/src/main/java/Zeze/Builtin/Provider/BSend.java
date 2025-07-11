// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public class BSend extends Zeze.Transaction.Bean implements BSendReadOnly {
    public static final long TYPEID = 545774009128015305L;

    private final Zeze.Transaction.Collections.PList1<Long> _linkSids;
    private long _protocolType;
    private Zeze.Net.Binary _protocolWholeData; // 完整的协议打包，包括了 type, size

    private static final java.lang.invoke.VarHandle vh_protocolType;
    private static final java.lang.invoke.VarHandle vh_protocolWholeData;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_protocolType = _l_.findVarHandle(BSend.class, "_protocolType", long.class);
            vh_protocolWholeData = _l_.findVarHandle(BSend.class, "_protocolWholeData", Zeze.Net.Binary.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    public Zeze.Transaction.Collections.PList1<Long> getLinkSids() {
        return _linkSids;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getLinkSidsReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_linkSids);
    }

    @Override
    public long getProtocolType() {
        if (!isManaged())
            return _protocolType;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _protocolType;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _protocolType;
    }

    public void setProtocolType(long _v_) {
        if (!isManaged()) {
            _protocolType = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_protocolType, _v_));
    }

    @Override
    public Zeze.Net.Binary getProtocolWholeData() {
        if (!isManaged())
            return _protocolWholeData;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _protocolWholeData;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _protocolWholeData;
    }

    public void setProtocolWholeData(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _protocolWholeData = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 3, vh_protocolWholeData, _v_));
    }

    @SuppressWarnings("deprecation")
    public BSend() {
        _linkSids = new Zeze.Transaction.Collections.PList1<>(Long.class);
        _linkSids.variableId(1);
        _protocolWholeData = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BSend(long _protocolType_, Zeze.Net.Binary _protocolWholeData_) {
        _linkSids = new Zeze.Transaction.Collections.PList1<>(Long.class);
        _linkSids.variableId(1);
        _protocolType = _protocolType_;
        if (_protocolWholeData_ == null)
            _protocolWholeData_ = Zeze.Net.Binary.Empty;
        _protocolWholeData = _protocolWholeData_;
    }

    @Override
    public void reset() {
        _linkSids.clear();
        setProtocolType(0);
        setProtocolWholeData(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Provider.BSend.Data toData() {
        var _d_ = new Zeze.Builtin.Provider.BSend.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Provider.BSend.Data)_o_);
    }

    public void assign(BSend.Data _o_) {
        _linkSids.clear();
        _linkSids.addAll(_o_._linkSids);
        setProtocolType(_o_._protocolType);
        setProtocolWholeData(_o_._protocolWholeData);
        _unknown_ = null;
    }

    public void assign(BSend _o_) {
        _linkSids.assign(_o_._linkSids);
        setProtocolType(_o_.getProtocolType());
        setProtocolWholeData(_o_.getProtocolWholeData());
        _unknown_ = _o_._unknown_;
    }

    public BSend copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSend copy() {
        var _c_ = new BSend();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSend _a_, BSend _b_) {
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
        _s_.append("Zeze.Builtin.Provider.BSend: {\n");
        _s_.append(_i1_).append("linkSids=[");
        if (!_linkSids.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _linkSids) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_linkSids.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("protocolType=").append(getProtocolType()).append(",\n");
        _s_.append(_i1_).append("protocolWholeData=").append(getProtocolWholeData()).append('\n');
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
            var _x_ = _linkSids;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
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
            long _x_ = getProtocolType();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getProtocolWholeData();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
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
            var _x_ = _linkSids;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setProtocolType(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setProtocolWholeData(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BSend))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSend)_o_;
        if (!_linkSids.equals(_b_._linkSids))
            return false;
        if (getProtocolType() != _b_.getProtocolType())
            return false;
        if (!getProtocolWholeData().equals(_b_.getProtocolWholeData()))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _linkSids.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _linkSids.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _linkSids) {
            if (_v_ < 0)
                return true;
        }
        if (getProtocolType() < 0)
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
                case 1: _linkSids.followerApply(_v_); break;
                case 2: _protocolType = _v_.longValue(); break;
                case 3: _protocolWholeData = _v_.binaryValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        Zeze.Serialize.Helper.decodeJsonList(_linkSids, Long.class, _r_.getString(_pn_ + "linkSids"));
        setProtocolType(_r_.getLong(_pn_ + "protocolType"));
        setProtocolWholeData(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "protocolWholeData")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "linkSids", Zeze.Serialize.Helper.encodeJson(_linkSids));
        _s_.appendLong(_pn_ + "protocolType", getProtocolType());
        _s_.appendBinary(_pn_ + "protocolWholeData", getProtocolWholeData());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "linkSids", "list", "", "long"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "protocolType", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "protocolWholeData", "binary", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 545774009128015305L;

    private java.util.ArrayList<Long> _linkSids;
    private long _protocolType;
    private Zeze.Net.Binary _protocolWholeData; // 完整的协议打包，包括了 type, size

    public java.util.ArrayList<Long> getLinkSids() {
        return _linkSids;
    }

    public void setLinkSids(java.util.ArrayList<Long> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _linkSids = _v_;
    }

    public long getProtocolType() {
        return _protocolType;
    }

    public void setProtocolType(long _v_) {
        _protocolType = _v_;
    }

    public Zeze.Net.Binary getProtocolWholeData() {
        return _protocolWholeData;
    }

    public void setProtocolWholeData(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _protocolWholeData = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _linkSids = new java.util.ArrayList<>();
        _protocolWholeData = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(java.util.ArrayList<Long> _linkSids_, long _protocolType_, Zeze.Net.Binary _protocolWholeData_) {
        if (_linkSids_ == null)
            _linkSids_ = new java.util.ArrayList<>();
        _linkSids = _linkSids_;
        _protocolType = _protocolType_;
        if (_protocolWholeData_ == null)
            _protocolWholeData_ = Zeze.Net.Binary.Empty;
        _protocolWholeData = _protocolWholeData_;
    }

    @Override
    public void reset() {
        _linkSids.clear();
        _protocolType = 0;
        _protocolWholeData = Zeze.Net.Binary.Empty;
    }

    @Override
    public Zeze.Builtin.Provider.BSend toBean() {
        var _b_ = new Zeze.Builtin.Provider.BSend();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BSend)_o_);
    }

    public void assign(BSend _o_) {
        _linkSids.clear();
        _linkSids.addAll(_o_._linkSids);
        _protocolType = _o_.getProtocolType();
        _protocolWholeData = _o_.getProtocolWholeData();
    }

    public void assign(BSend.Data _o_) {
        _linkSids.clear();
        _linkSids.addAll(_o_._linkSids);
        _protocolType = _o_._protocolType;
        _protocolWholeData = _o_._protocolWholeData;
    }

    @Override
    public BSend.Data copy() {
        var _c_ = new BSend.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSend.Data _a_, BSend.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BSend.Data clone() {
        return (BSend.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Provider.BSend: {\n");
        _s_.append(_i1_).append("linkSids=[");
        if (!_linkSids.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _linkSids) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_linkSids.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("protocolType=").append(_protocolType).append(",\n");
        _s_.append(_i1_).append("protocolWholeData=").append(_protocolWholeData).append('\n');
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
            var _x_ = _linkSids;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
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
            long _x_ = _protocolType;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _protocolWholeData;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _linkSids;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _protocolType = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _protocolWholeData = _o_.ReadBinary(_t_);
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
        if (!(_o_ instanceof BSend.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSend.Data)_o_;
        if (!_linkSids.equals(_b_._linkSids))
            return false;
        if (_protocolType != _b_._protocolType)
            return false;
        if (!_protocolWholeData.equals(_b_._protocolWholeData))
            return false;
        return true;
    }
}
}
