// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

/*
					Task rpc
					所有的TaskEvent均由这个rpc驱动（仿照现serverdev的结构）
					这个rpc的参数是BTaskEvent，内部的DynamicData是各个不同的任务的不同Event数据
*/
public interface BTaskEventReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTaskEvent copy();

    long getRoleId();
    Zeze.Transaction.DynamicBeanReadOnly getEventTypeReadOnly();
    Zeze.Builtin.Game.TaskBase.BSubmitTaskEventReadOnly getEventType_Zeze_Builtin_Game_TaskBase_BSubmitTaskEventReadOnly();
    Zeze.Builtin.Game.TaskBase.BAcceptTaskEventReadOnly getEventType_Zeze_Builtin_Game_TaskBase_BAcceptTaskEventReadOnly();
    Zeze.Builtin.Game.TaskBase.BSpecificTaskEventReadOnly getEventType_Zeze_Builtin_Game_TaskBase_BSpecificTaskEventReadOnly();
    Zeze.Builtin.Game.TaskBase.BBroadcastTaskEventReadOnly getEventType_Zeze_Builtin_Game_TaskBase_BBroadcastTaskEventReadOnly();
    Zeze.Transaction.DynamicBeanReadOnly getEventBeanReadOnly();
}
