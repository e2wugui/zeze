// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class taccount extends Zeze.Transaction.TableX<String, Zeze.Builtin.Online.BAccount, Zeze.Builtin.Online.BAccountReadOnly> {
    public taccount() {
        super("Zeze_Builtin_Online_taccount");
    }

    @Override
    public int getId() {
        return 1419906985;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_LastLoginVersion = 1;

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
    public Zeze.Builtin.Online.BAccount newValue() {
        return new Zeze.Builtin.Online.BAccount();
    }

    @Override
    public Zeze.Builtin.Online.BAccountReadOnly getReadOnly(String k) {
        return get(k);
    }
}
