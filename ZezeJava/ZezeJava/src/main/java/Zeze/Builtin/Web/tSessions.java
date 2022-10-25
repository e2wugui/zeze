// auto-generated @formatter:off
package Zeze.Builtin.Web;

import Zeze.Serialize.ByteBuffer;

// key is sessionid in cookie
@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tSessions extends Zeze.Transaction.TableX<String, Zeze.Builtin.Web.BSession, Zeze.Builtin.Web.BSessionReadOnly> {
    public tSessions() {
        super("Zeze_Builtin_Web_tSessions");
    }

    @Override
    public int getId() {
        return 1690882340;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_Account = 1;

    @Override
    public String decodeKey(ByteBuffer _os_) {
        String _v_;
        _v_ = _os_.ReadString();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(String _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _os_.WriteString(_v_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Web.BSession newValue() {
        return new Zeze.Builtin.Web.BSession();
    }

}
