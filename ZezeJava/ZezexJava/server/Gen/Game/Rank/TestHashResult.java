// auto-generated @formatter:off
package Game.Rank;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class TestHashResult extends Zeze.Transaction.Bean implements TestHashResultReadOnly {
    public static final long TYPEID = -5617696701337138972L;

    private int _hash;
    private int _out;
    private int _serverId;
    private boolean _b_oolean;
    private byte _b_yte;
    private short _s_hort;
    private float _f_loat;
    private long _l_ong;
    private double _d_ouble;
    private String _s_tring;
    private Zeze.Net.Binary _b_inary;
    private final Zeze.Transaction.Collections.CollOne<Game.Rank.TestToServerResult> _b_ean;
    private static final Zeze.Transaction.Collections.LogOneMeta<Game.Rank.TestToServerResult> meta1_b_ean
            = Zeze.Transaction.Collections.LogOneMeta.get(Game.Rank.TestToServerResult.class);

    private static final java.lang.invoke.VarHandle vh_hash;
    private static final java.lang.invoke.VarHandle vh_out;
    private static final java.lang.invoke.VarHandle vh_serverId;
    private static final java.lang.invoke.VarHandle vh_b_oolean;
    private static final java.lang.invoke.VarHandle vh_b_yte;
    private static final java.lang.invoke.VarHandle vh_s_hort;
    private static final java.lang.invoke.VarHandle vh_f_loat;
    private static final java.lang.invoke.VarHandle vh_l_ong;
    private static final java.lang.invoke.VarHandle vh_d_ouble;
    private static final java.lang.invoke.VarHandle vh_s_tring;
    private static final java.lang.invoke.VarHandle vh_b_inary;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_hash = _l_.findVarHandle(TestHashResult.class, "_hash", int.class);
            vh_out = _l_.findVarHandle(TestHashResult.class, "_out", int.class);
            vh_serverId = _l_.findVarHandle(TestHashResult.class, "_serverId", int.class);
            vh_b_oolean = _l_.findVarHandle(TestHashResult.class, "_b_oolean", boolean.class);
            vh_b_yte = _l_.findVarHandle(TestHashResult.class, "_b_yte", byte.class);
            vh_s_hort = _l_.findVarHandle(TestHashResult.class, "_s_hort", short.class);
            vh_f_loat = _l_.findVarHandle(TestHashResult.class, "_f_loat", float.class);
            vh_l_ong = _l_.findVarHandle(TestHashResult.class, "_l_ong", long.class);
            vh_d_ouble = _l_.findVarHandle(TestHashResult.class, "_d_ouble", double.class);
            vh_s_tring = _l_.findVarHandle(TestHashResult.class, "_s_tring", String.class);
            vh_b_inary = _l_.findVarHandle(TestHashResult.class, "_b_inary", Zeze.Net.Binary.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getHash() {
        if (!isManaged())
            return _hash;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _hash;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _hash;
    }

    public void setHash(int _v_) {
        if (!isManaged()) {
            _hash = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_hash, _v_));
    }

    @Override
    public int getOut() {
        if (!isManaged())
            return _out;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _out;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _out;
    }

    public void setOut(int _v_) {
        if (!isManaged()) {
            _out = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 2, vh_out, _v_));
    }

    @Override
    public int getServerId() {
        if (!isManaged())
            return _serverId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _serverId;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _serverId;
    }

    public void setServerId(int _v_) {
        if (!isManaged()) {
            _serverId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 3, vh_serverId, _v_));
    }

    @Override
    public boolean isB_oolean() {
        if (!isManaged())
            return _b_oolean;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _b_oolean;
        var log = (Zeze.Transaction.Logs.LogBool)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _b_oolean;
    }

    public void setB_oolean(boolean _v_) {
        if (!isManaged()) {
            _b_oolean = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBool(this, 4, vh_b_oolean, _v_));
    }

    @Override
    public byte getB_yte() {
        if (!isManaged())
            return _b_yte;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _b_yte;
        var log = (Zeze.Transaction.Logs.LogByte)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _b_yte;
    }

    public void setB_yte(byte _v_) {
        if (!isManaged()) {
            _b_yte = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogByte(this, 5, vh_b_yte, _v_));
    }

    @Override
    public short getS_hort() {
        if (!isManaged())
            return _s_hort;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _s_hort;
        var log = (Zeze.Transaction.Logs.LogShort)_t_.getLog(objectId() + 6);
        return log != null ? log.value : _s_hort;
    }

    public void setS_hort(short _v_) {
        if (!isManaged()) {
            _s_hort = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogShort(this, 6, vh_s_hort, _v_));
    }

    @Override
    public float getF_loat() {
        if (!isManaged())
            return _f_loat;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _f_loat;
        var log = (Zeze.Transaction.Logs.LogFloat)_t_.getLog(objectId() + 7);
        return log != null ? log.value : _f_loat;
    }

    public void setF_loat(float _v_) {
        if (!isManaged()) {
            _f_loat = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogFloat(this, 7, vh_f_loat, _v_));
    }

    @Override
    public long getL_ong() {
        if (!isManaged())
            return _l_ong;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _l_ong;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 8);
        return log != null ? log.value : _l_ong;
    }

    public void setL_ong(long _v_) {
        if (!isManaged()) {
            _l_ong = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 8, vh_l_ong, _v_));
    }

    @Override
    public double getD_ouble() {
        if (!isManaged())
            return _d_ouble;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _d_ouble;
        var log = (Zeze.Transaction.Logs.LogDouble)_t_.getLog(objectId() + 9);
        return log != null ? log.value : _d_ouble;
    }

    public void setD_ouble(double _v_) {
        if (!isManaged()) {
            _d_ouble = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogDouble(this, 9, vh_d_ouble, _v_));
    }

    @Override
    public String getS_tring() {
        if (!isManaged())
            return _s_tring;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _s_tring;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 10);
        return log != null ? log.stringValue() : _s_tring;
    }

    public void setS_tring(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _s_tring = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 10, vh_s_tring, _v_));
    }

    @Override
    public Zeze.Net.Binary getB_inary() {
        if (!isManaged())
            return _b_inary;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _b_inary;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 11);
        return log != null ? log.value : _b_inary;
    }

    public void setB_inary(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _b_inary = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 11, vh_b_inary, _v_));
    }

    public Game.Rank.TestToServerResult getB_ean() {
        return _b_ean.getValue();
    }

    public void setB_ean(Game.Rank.TestToServerResult _v_) {
        _b_ean.setValue(_v_);
    }

    @Override
    public Game.Rank.TestToServerResultReadOnly getB_eanReadOnly() {
        return _b_ean.getValue();
    }

    @SuppressWarnings("deprecation")
    public TestHashResult() {
        _s_tring = "";
        _b_inary = Zeze.Net.Binary.Empty;
        _b_ean = new Zeze.Transaction.Collections.CollOne<>(new Game.Rank.TestToServerResult(), meta1_b_ean);
        _b_ean.variableId(12);
    }

    @SuppressWarnings("deprecation")
    public TestHashResult(int _hash_, int _out_, int _serverId_, boolean _b_oolean_, byte _b_yte_, short _s_hort_, float _f_loat_, long _l_ong_, double _d_ouble_, @Nullable String _s_tring_, Zeze.Net.@Nullable Binary _b_inary_) {
        _hash = _hash_;
        _out = _out_;
        _serverId = _serverId_;
        _b_oolean = _b_oolean_;
        _b_yte = _b_yte_;
        _s_hort = _s_hort_;
        _f_loat = _f_loat_;
        _l_ong = _l_ong_;
        _d_ouble = _d_ouble_;
        if (_s_tring_ == null)
            _s_tring_ = "";
        _s_tring = _s_tring_;
        if (_b_inary_ == null)
            _b_inary_ = Zeze.Net.Binary.Empty;
        _b_inary = _b_inary_;
        _b_ean = new Zeze.Transaction.Collections.CollOne<>(new Game.Rank.TestToServerResult(), meta1_b_ean);
        _b_ean.variableId(12);
    }

    @Override
    public void reset() {
        setHash(0);
        setOut(0);
        setServerId(0);
        setB_oolean(false);
        setB_yte((byte)0);
        setS_hort((short)0);
        setF_loat(0);
        setL_ong(0);
        setD_ouble(0);
        setS_tring("");
        setB_inary(Zeze.Net.Binary.Empty);
        _b_ean.reset();
        _unknown_ = null;
    }

    public void assign(TestHashResult _o_) {
        setHash(_o_.getHash());
        setOut(_o_.getOut());
        setServerId(_o_.getServerId());
        setB_oolean(_o_.isB_oolean());
        setB_yte(_o_.getB_yte());
        setS_hort(_o_.getS_hort());
        setF_loat(_o_.getF_loat());
        setL_ong(_o_.getL_ong());
        setD_ouble(_o_.getD_ouble());
        setS_tring(_o_.getS_tring());
        setB_inary(_o_.getB_inary());
        _b_ean.assign(_o_._b_ean);
        _unknown_ = _o_._unknown_;
    }

    public TestHashResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public TestHashResult copy() {
        var _c_ = new TestHashResult();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(TestHashResult _a_, TestHashResult _b_) {
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
        _s_.append("Game.Rank.TestHashResult: {\n");
        _s_.append(_i1_).append("hash=").append(getHash()).append(",\n");
        _s_.append(_i1_).append("out=").append(getOut()).append(",\n");
        _s_.append(_i1_).append("serverId=").append(getServerId()).append(",\n");
        _s_.append(_i1_).append("b_oolean=").append(isB_oolean()).append(",\n");
        _s_.append(_i1_).append("b_yte=").append(getB_yte()).append(",\n");
        _s_.append(_i1_).append("s_hort=").append(getS_hort()).append(",\n");
        _s_.append(_i1_).append("f_loat=").append(getF_loat()).append(",\n");
        _s_.append(_i1_).append("l_ong=").append(getL_ong()).append(",\n");
        _s_.append(_i1_).append("d_ouble=").append(getD_ouble()).append(",\n");
        _s_.append(_i1_).append("s_tring=").append(getS_tring()).append(",\n");
        _s_.append(_i1_).append("b_inary=").append(getB_inary()).append(",\n");
        _s_.append(_i1_).append("b_ean=");
        _b_ean.buildString(_s_, _l_ + 8);
        _s_.append('\n');
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
            int _x_ = getHash();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getOut();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getServerId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            boolean _x_ = isB_oolean();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            int _x_ = getB_yte();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getS_hort();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            float _x_ = getF_loat();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.FLOAT);
                _o_.WriteFloat(_x_);
            }
        }
        {
            long _x_ = getL_ong();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            double _x_ = getD_ouble();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 9, ByteBuffer.DOUBLE);
                _o_.WriteDouble(_x_);
            }
        }
        {
            String _x_ = getS_tring();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 10, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getB_inary();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 11, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 12, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _b_ean.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
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
            setHash(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setOut(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setServerId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setB_oolean(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setB_yte(_o_.ReadByte(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setS_hort(_o_.ReadShort(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            setF_loat(_o_.ReadFloat(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 8) {
            setL_ong(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 9) {
            setD_ouble(_o_.ReadDouble(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 10) {
            setS_tring(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 11) {
            setB_inary(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 12) {
            _o_.ReadBean(_b_ean, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof TestHashResult))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (TestHashResult)_o_;
        if (getHash() != _b_.getHash())
            return false;
        if (getOut() != _b_.getOut())
            return false;
        if (getServerId() != _b_.getServerId())
            return false;
        if (isB_oolean() != _b_.isB_oolean())
            return false;
        if (getB_yte() != _b_.getB_yte())
            return false;
        if (getS_hort() != _b_.getS_hort())
            return false;
        if (getF_loat() != _b_.getF_loat())
            return false;
        if (getL_ong() != _b_.getL_ong())
            return false;
        if (getD_ouble() != _b_.getD_ouble())
            return false;
        if (!getS_tring().equals(_b_.getS_tring()))
            return false;
        if (!getB_inary().equals(_b_.getB_inary()))
            return false;
        if (!_b_ean.equals(_b_._b_ean))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _b_ean.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _b_ean.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getHash() < 0)
            return true;
        if (getOut() < 0)
            return true;
        if (getServerId() < 0)
            return true;
        if (getS_hort() < 0)
            return true;
        if (getL_ong() < 0)
            return true;
        if (_b_ean.negativeCheck())
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
                case 1: _hash = _v_.intValue(); break;
                case 2: _out = _v_.intValue(); break;
                case 3: _serverId = _v_.intValue(); break;
                case 4: _b_oolean = _v_.booleanValue(); break;
                case 5: _b_yte = _v_.byteValue(); break;
                case 6: _s_hort = _v_.shortValue(); break;
                case 7: _f_loat = _v_.floatValue(); break;
                case 8: _l_ong = _v_.longValue(); break;
                case 9: _d_ouble = _v_.doubleValue(); break;
                case 10: _s_tring = _v_.stringValue(); break;
                case 11: _b_inary = _v_.binaryValue(); break;
                case 12: _b_ean.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setHash(_r_.getInt(_pn_ + "hash"));
        setOut(_r_.getInt(_pn_ + "out"));
        setServerId(_r_.getInt(_pn_ + "serverId"));
        setB_oolean(_r_.getBoolean(_pn_ + "b_oolean"));
        setB_yte(_r_.getByte(_pn_ + "b_yte"));
        setS_hort(_r_.getShort(_pn_ + "s_hort"));
        setF_loat(_r_.getFloat(_pn_ + "f_loat"));
        setL_ong(_r_.getLong(_pn_ + "l_ong"));
        setD_ouble(_r_.getDouble(_pn_ + "d_ouble"));
        setS_tring(_r_.getString(_pn_ + "s_tring"));
        if (getS_tring() == null)
            setS_tring("");
        setB_inary(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "b_inary")));
        _p_.add("b_ean");
        _b_ean.decodeResultSet(_p_, _r_);
        _p_.removeLast();
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "hash", getHash());
        _s_.appendInt(_pn_ + "out", getOut());
        _s_.appendInt(_pn_ + "serverId", getServerId());
        _s_.appendBoolean(_pn_ + "b_oolean", isB_oolean());
        _s_.appendByte(_pn_ + "b_yte", getB_yte());
        _s_.appendShort(_pn_ + "s_hort", getS_hort());
        _s_.appendFloat(_pn_ + "f_loat", getF_loat());
        _s_.appendLong(_pn_ + "l_ong", getL_ong());
        _s_.appendDouble(_pn_ + "d_ouble", getD_ouble());
        _s_.appendString(_pn_ + "s_tring", getS_tring());
        _s_.appendBinary(_pn_ + "b_inary", getB_inary());
        _p_.add("b_ean");
        _b_ean.encodeSQLStatement(_p_, _s_);
        _p_.removeLast();
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "hash", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "out", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "serverId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "b_oolean", "bool", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "b_yte", "byte", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "s_hort", "short", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "f_loat", "float", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(8, "l_ong", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(9, "d_ouble", "double", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(10, "s_tring", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(11, "b_inary", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(12, "b_ean", "Game.Rank.TestToServerResult", "", ""));
        return _v_;
    }
}
