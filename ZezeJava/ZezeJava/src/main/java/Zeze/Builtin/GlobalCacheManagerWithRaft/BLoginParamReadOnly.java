// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

public interface BLoginParamReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BLoginParam copy();

    public int getServerId();
    public int getGlobalCacheManagerHashIndex();
    public boolean isDebugMode();
}
