// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

public interface BLoginParamReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BLoginParam copy();

    int getServerId();
    int getGlobalCacheManagerHashIndex();
    boolean isDebugMode();
}
