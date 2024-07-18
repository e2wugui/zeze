// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public class BSend extends Zeze.Transaction.Bean implements BSendReadOnly {
    public static final long TYPEID = 545774009128015305L;

    private final Zeze.Transaction.Collections.PList1<Long> _linkSids;
    private long _protocolType;
    private Zeze.Net.Binary _protocolWholeData; // 完整的协议打包，包括了 type, size

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
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _protocolType;
        var log = (Log__protocolType)txn.getLog(objectId() + 2);
        return log != null ? log.value : _protocolType;
    }

    public void setProtocolType(long value) {
        if (!isManaged()) {
            _protocolType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__protocolType(this, 2, value));
    }

    @Override
    public Zeze.Net.Binary getProtocolWholeData() {
        if (!isManaged())
            return _protocolWholeData;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _protocolWholeData;
        var log = (Log__protocolWholeData)txn.getLog(objectId() + 3);
        return log != null ? log.value : _protocolWholeData;
    }

    public void setProtocolWholeData(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _protocolWholeData = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__protocolWholeData(this, 3, value));
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
        var data = new Zeze.Builtin.Provider.BSend.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Provider.BSend.Data)other);
    }

    public void assign(BSend.Data other) {
        _linkSids.clear();
        _linkSids.addAll(other._linkSids);
        setProtocolType(other._protocolType);
        setProtocolWholeData(other._protocolWholeData);
        _unknown_ = null;
    }

    public void assign(BSend other) {
        _linkSids.assign(other._linkSids);
        setProtocolType(other.getProtocolType());
        setProtocolWholeData(other.getProtocolWholeData());
        _unknown_ = other._unknown_;
    }

    public BSend copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSend copy() {
        var copy = new BSend();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSend a, BSend b) {
        BSend save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__protocolType extends Zeze.Transaction.Logs.LogLong {
        public Log__protocolType(BSend bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSend)getBelong())._protocolType = value; }
    }

    private static final class Log__protocolWholeData extends Zeze.Transaction.Logs.LogBinary {
        public Log__protocolWholeData(BSend bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSend)getBelong())._protocolWholeData = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BSend: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("linkSids=[");
        if (!_linkSids.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _linkSids) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("protocolType=").append(getProtocolType()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("protocolWholeData=").append(getProtocolWholeData()).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
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
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _linkSids.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _linkSids.initRootInfoWithRedo(root, this);
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
    public void followerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _linkSids.followerApply(vlog); break;
                case 2: _protocolType = vlog.longValue(); break;
                case 3: _protocolWholeData = vlog.binaryValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonList(_linkSids, Long.class, rs.getString(_parents_name_ + "linkSids"));
        setProtocolType(rs.getLong(_parents_name_ + "protocolType"));
        setProtocolWholeData(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "protocolWholeData")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "linkSids", Zeze.Serialize.Helper.encodeJson(_linkSids));
        st.appendLong(_parents_name_ + "protocolType", getProtocolType());
        st.appendBinary(_parents_name_ + "protocolWholeData", getProtocolWholeData());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "linkSids", "list", "", "long"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "protocolType", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "protocolWholeData", "binary", "", ""));
        return vars;
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

    public void setLinkSids(java.util.ArrayList<Long> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _linkSids = value;
    }

    public long getProtocolType() {
        return _protocolType;
    }

    public void setProtocolType(long value) {
        _protocolType = value;
    }

    public Zeze.Net.Binary getProtocolWholeData() {
        return _protocolWholeData;
    }

    public void setProtocolWholeData(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _protocolWholeData = value;
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
        var bean = new Zeze.Builtin.Provider.BSend();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BSend)other);
    }

    public void assign(BSend other) {
        _linkSids.clear();
        _linkSids.addAll(other._linkSids);
        _protocolType = other.getProtocolType();
        _protocolWholeData = other.getProtocolWholeData();
    }

    public void assign(BSend.Data other) {
        _linkSids.clear();
        _linkSids.addAll(other._linkSids);
        _protocolType = other._protocolType;
        _protocolWholeData = other._protocolWholeData;
    }

    @Override
    public BSend.Data copy() {
        var copy = new BSend.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSend.Data a, BSend.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
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
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BSend: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("linkSids=[");
        if (!_linkSids.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _linkSids) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("protocolType=").append(_protocolType).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("protocolWholeData=").append(_protocolWholeData).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

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
}
}
