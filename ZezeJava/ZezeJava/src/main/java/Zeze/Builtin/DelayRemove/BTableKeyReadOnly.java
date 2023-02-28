// auto-generated @formatter:off
package Zeze.Builtin.DelayRemove;

public interface BTableKeyReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTableKey copy();

    String getTableName();
    Zeze.Net.Binary getEncodedKey();
    long getEnqueueTime();
}
