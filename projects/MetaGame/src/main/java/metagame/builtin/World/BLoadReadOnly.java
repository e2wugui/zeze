// auto-generated @formatter:off
package metagame.builtin.World;

// 地图实例（线）的负载
public interface BLoadReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BLoad copy();

    int getPlayerCount();
    long getComputeCount();
    long getComputeCountPS();
    long getComputeCountLast();
    long getComputeCountTime();
}
