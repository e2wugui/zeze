// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BLocal extends Zeze.Transaction.Bean implements BLocalReadOnly {
    public static final long TYPEID = -6330089022826554666L;

    private long _LoginVersion;
    private final Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Online.BAny> _Datas;

    private transient Object __zeze_map_key__;

    @Override
    public Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object value) {
        __zeze_map_key__ = value;
    }

    @Override
    public long getLoginVersion() {
        if (!isManaged())
            return _LoginVersion;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _LoginVersion;
        var log = (Log__LoginVersion)txn.getLog(objectId() + 1);
        return log != null ? log.value : _LoginVersion;
    }

    public void setLoginVersion(long value) {
        if (!isManaged()) {
            _LoginVersion = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__LoginVersion(this, 1, value));
    }

    public Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Online.BAny> getDatas() {
        return _Datas;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Online.BAny, Zeze.Builtin.Online.BAnyReadOnly> getDatasReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Datas);
    }

    @SuppressWarnings("deprecation")
    public BLocal() {
        _Datas = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Builtin.Online.BAny.class);
        _Datas.variableId(2);
    }

    @SuppressWarnings("deprecation")
    public BLocal(long _LoginVersion_) {
        _LoginVersion = _LoginVersion_;
        _Datas = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Builtin.Online.BAny.class);
        _Datas.variableId(2);
    }

    @Override
    public void reset() {
        setLoginVersion(0);
        _Datas.clear();
        _unknown_ = null;
    }

    public void assign(BLocal other) {
        setLoginVersion(other.getLoginVersion());
        _Datas.clear();
        for (var e : other._Datas.entrySet())
            _Datas.put(e.getKey(), e.getValue().copy());
        _unknown_ = other._unknown_;
    }

    public BLocal copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLocal copy() {
        var copy = new BLocal();
        copy.assign(this);
        return copy;
    }

    public static void swap(BLocal a, BLocal b) {
        BLocal save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__LoginVersion extends Zeze.Transaction.Logs.LogLong {
        public Log__LoginVersion(BLocal bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLocal)getBelong())._LoginVersion = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Online.BLocal: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("LoginVersion=").append(getLoginVersion()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Datas={");
        if (!_Datas.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Datas.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(System.lineSeparator());
                _kv_.getValue().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(System.lineSeparator());
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
            long _x_ = getLoginVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Datas;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _e_.getValue().encode(_o_);
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
            setLoginVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Datas;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Online.BAny(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Datas.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Datas.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getLoginVersion() < 0)
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
                case 1: _LoginVersion = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _Datas.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setLoginVersion(rs.getLong(_parents_name_ + "LoginVersion"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "Datas", _Datas, rs.getString(_parents_name_ + "Datas"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "LoginVersion", getLoginVersion());
        st.appendString(_parents_name_ + "Datas", Zeze.Serialize.Helper.encodeJson(_Datas));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "LoginVersion", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Datas", "map", "string", "Zeze.Builtin.Online.BAny"));
        return vars;
    }
}
