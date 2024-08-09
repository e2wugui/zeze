// auto-generated @formatter:off
package Zeze.Builtin.HotDistribute;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BOpenFile extends Zeze.Transaction.Bean implements BOpenFileReadOnly {
    public static final long TYPEID = 6565966142026805362L;

    private String _FileName;

    private static final java.lang.invoke.VarHandle vh_FileName;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_FileName = _l_.findVarHandle(BOpenFile.class, "_FileName", String.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getFileName() {
        if (!isManaged())
            return _FileName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _FileName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _FileName;
    }

    public void setFileName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _FileName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_FileName, _v_));
    }

    @SuppressWarnings("deprecation")
    public BOpenFile() {
        _FileName = "";
    }

    @SuppressWarnings("deprecation")
    public BOpenFile(String _FileName_) {
        if (_FileName_ == null)
            _FileName_ = "";
        _FileName = _FileName_;
    }

    @Override
    public void reset() {
        setFileName("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.HotDistribute.BOpenFile.Data toData() {
        var _d_ = new Zeze.Builtin.HotDistribute.BOpenFile.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.HotDistribute.BOpenFile.Data)_o_);
    }

    public void assign(BOpenFile.Data _o_) {
        setFileName(_o_._FileName);
        _unknown_ = null;
    }

    public void assign(BOpenFile _o_) {
        setFileName(_o_.getFileName());
        _unknown_ = _o_._unknown_;
    }

    public BOpenFile copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BOpenFile copy() {
        var _c_ = new BOpenFile();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BOpenFile _a_, BOpenFile _b_) {
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
        _s_.append("Zeze.Builtin.HotDistribute.BOpenFile: {\n");
        _s_.append(_i1_).append("FileName=").append(getFileName()).append('\n');
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
            String _x_ = getFileName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
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
            setFileName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BOpenFile))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BOpenFile)_o_;
        if (!getFileName().equals(_b_.getFileName()))
            return false;
        return true;
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
                case 1: _FileName = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setFileName(_r_.getString(_pn_ + "FileName"));
        if (getFileName() == null)
            setFileName("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "FileName", getFileName());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "FileName", "string", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 6565966142026805362L;

    private String _FileName;

    public String getFileName() {
        return _FileName;
    }

    public void setFileName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _FileName = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _FileName = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _FileName_) {
        if (_FileName_ == null)
            _FileName_ = "";
        _FileName = _FileName_;
    }

    @Override
    public void reset() {
        _FileName = "";
    }

    @Override
    public Zeze.Builtin.HotDistribute.BOpenFile toBean() {
        var _b_ = new Zeze.Builtin.HotDistribute.BOpenFile();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BOpenFile)_o_);
    }

    public void assign(BOpenFile _o_) {
        _FileName = _o_.getFileName();
    }

    public void assign(BOpenFile.Data _o_) {
        _FileName = _o_._FileName;
    }

    @Override
    public BOpenFile.Data copy() {
        var _c_ = new BOpenFile.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BOpenFile.Data _a_, BOpenFile.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BOpenFile.Data clone() {
        return (BOpenFile.Data)super.clone();
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
        _s_.append("Zeze.Builtin.HotDistribute.BOpenFile: {\n");
        _s_.append(_i1_).append("FileName=").append(_FileName).append('\n');
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
            String _x_ = _FileName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
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
            _FileName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
