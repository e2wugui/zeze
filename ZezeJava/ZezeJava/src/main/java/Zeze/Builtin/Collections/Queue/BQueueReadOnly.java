// auto-generated @formatter:off
package Zeze.Builtin.Collections.Queue;

/*
				1. 单向链表。2. Value没有索引。3. 每个Value记录加入的时间。4. 只能从Head提取，从Tail添加。5. 用作Stack时也可以从Head添加。
				链表结构: (NewStackNode -＞) Head -＞ ... -＞ Tail (-＞ NewQueueNode)。
				第一个用户是Table.GC，延迟删除记录。
*/
public interface BQueueReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BQueue copy();

    long getHeadNodeId();
    long getTailNodeId();
    long getCount();
    long getLastNodeId();
    long getLoadSerialNo();
}
