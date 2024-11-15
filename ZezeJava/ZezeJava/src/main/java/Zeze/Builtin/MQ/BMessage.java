// auto-generated @formatter:off
package Zeze.Builtin.MQ;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BMessage extends Zeze.Transaction.Bean implements BMessageReadOnly {
    public static final long TYPEID = -6688505362992437637L;

    private String _MessageId;
    private Zeze.Net.Binary _Body;

    private static final java.lang.invoke.VarHandle vh_MessageId;
    private static final java.lang.invoke.VarHandle vh_Body;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_MessageId = _l_.findVarHandle(BMessage.class, "_MessageId", String.class);
            vh_Body = _l_.findVarHandle(BMessage.class, "_Body", Zeze.Net.Binary.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getMessageId() {
        if (!isManaged())
            return _MessageId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _MessageId;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _MessageId;
    }

    public void setMessageId(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _MessageId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_MessageId, _v_));
    }

    @Override
    public Zeze.Net.Binary getBody() {
        if (!isManaged())
            return _Body;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Body;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Body;
    }

    public void setBody(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Body = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 2, vh_Body, _v_));
    }

    @SuppressWarnings("deprecation")
    public BMessage() {
        _MessageId = "";
        _Body = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BMessage(String _MessageId_, Zeze.Net.Binary _Body_) {
        if (_MessageId_ == null)
            _MessageId_ = "";
        _MessageId = _MessageId_;
        if (_Body_ == null)
            _Body_ = Zeze.Net.Binary.Empty;
        _Body = _Body_;
    }

    @Override
    public void reset() {
        setMessageId("");
        setBody(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.MQ.BMessage.Data toData() {
        var _d_ = new Zeze.Builtin.MQ.BMessage.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.MQ.BMessage.Data)_o_);
    }

    public void assign(BMessage.Data _o_) {
        setMessageId(_o_._MessageId);
        setBody(_o_._Body);
        _unknown_ = null;
    }

    public void assign(BMessage _o_) {
        setMessageId(_o_.getMessageId());
        setBody(_o_.getBody());
        _unknown_ = _o_._unknown_;
    }

    public BMessage copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BMessage copy() {
        var _c_ = new BMessage();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BMessage _a_, BMessage _b_) {
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
        _s_.append("Zeze.Builtin.MQ.BMessage: {\n");
        _s_.append(_i1_).append("MessageId=").append(getMessageId()).append(",\n");
        _s_.append(_i1_).append("Body=").append(getBody()).append('\n');
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
            String _x_ = getMessageId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getBody();
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
            setMessageId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setBody(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BMessage))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BMessage)_o_;
        if (!getMessageId().equals(_b_.getMessageId()))
            return false;
        if (!getBody().equals(_b_.getBody()))
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
                case 1: _MessageId = _v_.stringValue(); break;
                case 2: _Body = _v_.binaryValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setMessageId(_r_.getString(_pn_ + "MessageId"));
        if (getMessageId() == null)
            setMessageId("");
        setBody(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "Body")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "MessageId", getMessageId());
        _s_.appendBinary(_pn_ + "Body", getBody());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "MessageId", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Body", "binary", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -6688505362992437637L;

    private String _MessageId;
    private Zeze.Net.Binary _Body;

    public String getMessageId() {
        return _MessageId;
    }

    public void setMessageId(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _MessageId = _v_;
    }

    public Zeze.Net.Binary getBody() {
        return _Body;
    }

    public void setBody(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Body = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _MessageId = "";
        _Body = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(String _MessageId_, Zeze.Net.Binary _Body_) {
        if (_MessageId_ == null)
            _MessageId_ = "";
        _MessageId = _MessageId_;
        if (_Body_ == null)
            _Body_ = Zeze.Net.Binary.Empty;
        _Body = _Body_;
    }

    @Override
    public void reset() {
        _MessageId = "";
        _Body = Zeze.Net.Binary.Empty;
    }

    @Override
    public Zeze.Builtin.MQ.BMessage toBean() {
        var _b_ = new Zeze.Builtin.MQ.BMessage();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BMessage)_o_);
    }

    public void assign(BMessage _o_) {
        _MessageId = _o_.getMessageId();
        _Body = _o_.getBody();
    }

    public void assign(BMessage.Data _o_) {
        _MessageId = _o_._MessageId;
        _Body = _o_._Body;
    }

    @Override
    public BMessage.Data copy() {
        var _c_ = new BMessage.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BMessage.Data _a_, BMessage.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BMessage.Data clone() {
        return (BMessage.Data)super.clone();
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
        _s_.append("Zeze.Builtin.MQ.BMessage: {\n");
        _s_.append(_i1_).append("MessageId=").append(_MessageId).append(",\n");
        _s_.append(_i1_).append("Body=").append(_Body).append('\n');
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
            String _x_ = _MessageId;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _Body;
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
            _MessageId = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Body = _o_.ReadBinary(_t_);
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
        if (!(_o_ instanceof BMessage.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BMessage.Data)_o_;
        if (!_MessageId.equals(_b_._MessageId))
            return false;
        if (!_Body.equals(_b_._Body))
            return false;
        return true;
    }
}
}
