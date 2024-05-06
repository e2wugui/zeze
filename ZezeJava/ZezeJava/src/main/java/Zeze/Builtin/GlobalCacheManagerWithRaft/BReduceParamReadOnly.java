// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

public interface BReduceParamReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BReduceParam copy();

    Zeze.Net.Binary getGlobalKey();
    int getState();
    long getReduceTid();
}
