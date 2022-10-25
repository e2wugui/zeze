// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tversion extends Zeze.Transaction.TableX<String, Zeze.Builtin.Online.BVersions, Zeze.Builtin.Online.BVersionsReadOnly> {
    public tversion() {
        super("Zeze_Builtin_Online_tversion");
    }

    @Override
    public int getId() {
        return -1179546366;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_Logins = 1;

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
    public Zeze.Builtin.Online.BVersions newValue() {
        return new Zeze.Builtin.Online.BVersions();
    }

    public Zeze.Builtin.Online.BVersionsReadOnly getReadOnly(String k) {
        return get(k);
    }
}
