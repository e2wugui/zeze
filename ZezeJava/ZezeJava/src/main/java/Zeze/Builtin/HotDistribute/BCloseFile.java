// auto-generated @formatter:off
package Zeze.Builtin.HotDistribute;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BCloseFile extends Zeze.Transaction.Bean implements BCloseFileReadOnly {
    public static final long TYPEID = -2195521187339200956L;

    private String _FileName;
    private Zeze.Net.Binary _Md5;

    private static final java.lang.invoke.VarHandle vh_FileName;
    private static final java.lang.invoke.VarHandle vh_Md5;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_FileName = _l_.findVarHandle(BCloseFile.class, "_FileName", String.class);
            vh_Md5 = _l_.findVarHandle(BCloseFile.class, "_Md5", Zeze.Net.Binary.class);
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
        return log != null ? log.stringValue() : _FileName;
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

    @Override
    public Zeze.Net.Binary getMd5() {
        if (!isManaged())
            return _Md5;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Md5;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Md5;
    }

    public void setMd5(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Md5 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 2, vh_Md5, _v_));
    }

    @SuppressWarnings("deprecation")
    public BCloseFile() {
        _FileName = "";
        _Md5 = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BCloseFile(String _FileName_, Zeze.Net.Binary _Md5_) {
        if (_FileName_ == null)
            _FileName_ = "";
        _FileName = _FileName_;
        if (_Md5_ == null)
            _Md5_ = Zeze.Net.Binary.Empty;
        _Md5 = _Md5_;
    }

    @Override
    public void reset() {
        setFileName("");
        setMd5(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.HotDistribute.BCloseFile.Data toData() {
        var _d_ = new Zeze.Builtin.HotDistribute.BCloseFile.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.HotDistribute.BCloseFile.Data)_o_);
    }

    public void assign(BCloseFile.Data _o_) {
        setFileName(_o_._FileName);
        setMd5(_o_._Md5);
        _unknown_ = null;
    }

    public void assign(BCloseFile _o_) {
        setFileName(_o_.getFileName());
        setMd5(_o_.getMd5());
        _unknown_ = _o_._unknown_;
    }

    public BCloseFile copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCloseFile copy() {
        var _c_ = new BCloseFile();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BCloseFile _a_, BCloseFile _b_) {
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
        _s_.append("Zeze.Builtin.HotDistribute.BCloseFile: {\n");
        _s_.append(_i1_).append("FileName=").append(getFileName()).append(",\n");
        _s_.append(_i1_).append("Md5=").append(getMd5()).append('\n');
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
        {
            var _x_ = getMd5();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
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
            setFileName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setMd5(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BCloseFile))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BCloseFile)_o_;
        if (!getFileName().equals(_b_.getFileName()))
            return false;
        if (!getMd5().equals(_b_.getMd5()))
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
                case 2: _Md5 = _v_.binaryValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setFileName(_r_.getString(_pn_ + "FileName"));
        if (getFileName() == null)
            setFileName("");
        setMd5(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "Md5")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "FileName", getFileName());
        _s_.appendBinary(_pn_ + "Md5", getMd5());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "FileName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Md5", "binary", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -2195521187339200956L;

    private String _FileName;
    private Zeze.Net.Binary _Md5;

    public String getFileName() {
        return _FileName;
    }

    public void setFileName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _FileName = _v_;
    }

    public Zeze.Net.Binary getMd5() {
        return _Md5;
    }

    public void setMd5(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Md5 = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _FileName = "";
        _Md5 = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(String _FileName_, Zeze.Net.Binary _Md5_) {
        if (_FileName_ == null)
            _FileName_ = "";
        _FileName = _FileName_;
        if (_Md5_ == null)
            _Md5_ = Zeze.Net.Binary.Empty;
        _Md5 = _Md5_;
    }

    @Override
    public void reset() {
        _FileName = "";
        _Md5 = Zeze.Net.Binary.Empty;
    }

    @Override
    public Zeze.Builtin.HotDistribute.BCloseFile toBean() {
        var _b_ = new Zeze.Builtin.HotDistribute.BCloseFile();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BCloseFile)_o_);
    }

    public void assign(BCloseFile _o_) {
        _FileName = _o_.getFileName();
        _Md5 = _o_.getMd5();
    }

    public void assign(BCloseFile.Data _o_) {
        _FileName = _o_._FileName;
        _Md5 = _o_._Md5;
    }

    @Override
    public BCloseFile.Data copy() {
        var _c_ = new BCloseFile.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BCloseFile.Data _a_, BCloseFile.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BCloseFile.Data clone() {
        return (BCloseFile.Data)super.clone();
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
        _s_.append("Zeze.Builtin.HotDistribute.BCloseFile: {\n");
        _s_.append(_i1_).append("FileName=").append(_FileName).append(",\n");
        _s_.append(_i1_).append("Md5=").append(_Md5).append('\n');
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
        {
            var _x_ = _Md5;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
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
            _FileName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Md5 = _o_.ReadBinary(_t_);
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
        if (!(_o_ instanceof BCloseFile.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BCloseFile.Data)_o_;
        if (!_FileName.equals(_b_._FileName))
            return false;
        if (!_Md5.equals(_b_._Md5))
            return false;
        return true;
    }
}
}
