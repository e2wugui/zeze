// auto-generated @formatter:off
package Zeze.Builtin.MQ.Master;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BMQServers extends Zeze.Transaction.Bean implements BMQServersReadOnly {
    public static final long TYPEID = 723031994174062842L;

    private final Zeze.Transaction.Collections.PList2<Zeze.Builtin.MQ.Master.BMQServer> _Servers; // Subscribe可能返回多个地址。

    public Zeze.Transaction.Collections.PList2<Zeze.Builtin.MQ.Master.BMQServer> getServers() {
        return _Servers;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.MQ.Master.BMQServer, Zeze.Builtin.MQ.Master.BMQServerReadOnly> getServersReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_Servers);
    }

    @SuppressWarnings("deprecation")
    public BMQServers() {
        _Servers = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.MQ.Master.BMQServer.class);
        _Servers.variableId(1);
    }

    @Override
    public void reset() {
        _Servers.clear();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.MQ.Master.BMQServers.Data toData() {
        var _d_ = new Zeze.Builtin.MQ.Master.BMQServers.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.MQ.Master.BMQServers.Data)_o_);
    }

    public void assign(BMQServers.Data _o_) {
        _Servers.clear();
        for (var _e_ : _o_._Servers) {
            var _v_ = new Zeze.Builtin.MQ.Master.BMQServer();
            _v_.assign(_e_);
            _Servers.add(_v_);
        }
        _unknown_ = null;
    }

    public void assign(BMQServers _o_) {
        _Servers.clear();
        for (var _e_ : _o_._Servers)
            _Servers.add(_e_.copy());
        _unknown_ = _o_._unknown_;
    }

    public BMQServers copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BMQServers copy() {
        var _c_ = new BMQServers();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BMQServers _a_, BMQServers _b_) {
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
        _s_.append("Zeze.Builtin.MQ.Master.BMQServers: {\n");
        _s_.append(_i1_).append("Servers=[");
        if (!_Servers.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _Servers) {
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
            var _x_ = _Servers;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
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
            var _x_ = _Servers;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.MQ.Master.BMQServer(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BMQServers))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BMQServers)_o_;
        if (!_Servers.equals(_b_._Servers))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Servers.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Servers.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _Servers) {
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
                case 1: _Servers.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        Zeze.Serialize.Helper.decodeJsonList(_Servers, Zeze.Builtin.MQ.Master.BMQServer.class, _r_.getString(_pn_ + "Servers"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Servers", Zeze.Serialize.Helper.encodeJson(_Servers));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Servers", "list", "", "Zeze.Builtin.MQ.Master.BMQServer"));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 723031994174062842L;

    private java.util.ArrayList<Zeze.Builtin.MQ.Master.BMQServer.Data> _Servers; // Subscribe可能返回多个地址。

    public java.util.ArrayList<Zeze.Builtin.MQ.Master.BMQServer.Data> getServers() {
        return _Servers;
    }

    public void setServers(java.util.ArrayList<Zeze.Builtin.MQ.Master.BMQServer.Data> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Servers = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Servers = new java.util.ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    public Data(java.util.ArrayList<Zeze.Builtin.MQ.Master.BMQServer.Data> _Servers_) {
        if (_Servers_ == null)
            _Servers_ = new java.util.ArrayList<>();
        _Servers = _Servers_;
    }

    @Override
    public void reset() {
        _Servers.clear();
    }

    @Override
    public Zeze.Builtin.MQ.Master.BMQServers toBean() {
        var _b_ = new Zeze.Builtin.MQ.Master.BMQServers();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BMQServers)_o_);
    }

    public void assign(BMQServers _o_) {
        _Servers.clear();
        for (var _e_ : _o_._Servers) {
            var _v_ = new Zeze.Builtin.MQ.Master.BMQServer.Data();
            _v_.assign(_e_);
            _Servers.add(_v_);
        }
    }

    public void assign(BMQServers.Data _o_) {
        _Servers.clear();
        for (var _e_ : _o_._Servers)
            _Servers.add(_e_.copy());
    }

    @Override
    public BMQServers.Data copy() {
        var _c_ = new BMQServers.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BMQServers.Data _a_, BMQServers.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BMQServers.Data clone() {
        return (BMQServers.Data)super.clone();
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
        _s_.append("Zeze.Builtin.MQ.Master.BMQServers: {\n");
        _s_.append(_i1_).append("Servers=[");
        if (!_Servers.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _Servers) {
                _s_.append(_i2_).append("Item=");
                _v_.buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("]\n");
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
            var _x_ = _Servers;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (int _j_ = 0, _c_ = _x_.size(); _j_ < _c_; _j_++) {
                    var _v_ = _x_.get(_j_);
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _Servers;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.MQ.Master.BMQServer.Data(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
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
        if (!(_o_ instanceof BMQServers.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BMQServers.Data)_o_;
        if (!_Servers.equals(_b_._Servers))
            return false;
        return true;
    }
}
}
