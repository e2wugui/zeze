// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

// 内置条件类型：CollectCoinTask
public interface BCollectCoinTaskReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BCollectCoinTask copy();

    public String getName();
    public long getTargetCoinCount();
    public long getCurrentCoinCount();
}
