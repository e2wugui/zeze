// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

/*
					Task rpc
					所有的TaskEvent均由这个rpc驱动（仿照现serverdev的结构）
					这个rpc的参数是BTaskEvent，内部的DynamicData是各个不同的任务的不同Bean数据
*/
public interface BTaskEventReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTaskEvent copy();

    public long getRoleId();
    public Zeze.Transaction.DynamicBeanReadOnly getExtendedDataReadOnly();

}
