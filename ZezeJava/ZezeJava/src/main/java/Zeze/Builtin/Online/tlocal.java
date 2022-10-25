// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tlocal extends Zeze.Transaction.TableX<String, Zeze.Builtin.Online.BLocals, Zeze.Builtin.Online.BLocalsReadOnly> {
    public tlocal() {
        super("Zeze_Builtin_Online_tlocal");
    }

    @Override
    public int getId() {
        return -1858917951;
    }

    @Override
    public boolean isMemory() {
        return true;
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
    public Zeze.Builtin.Online.BLocals newValue() {
        return new Zeze.Builtin.Online.BLocals();
    }

    public Zeze.Builtin.Online.BLocalsReadOnly getReadOnly(String k) {
        return get(k);
    }
}
