// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tonline extends Zeze.Transaction.TableX<String, Zeze.Builtin.Online.BOnlines, Zeze.Builtin.Online.BOnlinesReadOnly> {
    public tonline() {
        super("Zeze_Builtin_Online_tonline");
    }

    @Override
    public int getId() {
        return -2094601796;
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
    public Zeze.Builtin.Online.BOnlines newValue() {
        return new Zeze.Builtin.Online.BOnlines();
    }

}
