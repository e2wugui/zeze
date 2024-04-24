// auto-generated @formatter:off
package Zeze.Builtin.HttpSession;

public interface BSessionValueReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSessionValue copy();

    long getCreateTime();
    long getExpireTime();
    Zeze.Transaction.Collections.PMap1ReadOnly<String, String> getPropertiesReadOnly();
}
