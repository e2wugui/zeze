// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

// rpc
public interface BAcquireParamReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BAcquireParam copy();

    Zeze.Net.Binary getGlobalKey();
    int getState();
}
