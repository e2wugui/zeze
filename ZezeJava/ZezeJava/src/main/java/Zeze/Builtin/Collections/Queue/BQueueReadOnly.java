// auto-generated @formatter:off
package Zeze.Builtin.Collections.Queue;

/*
				1. 单向链表。2. Value没有索引。3. 每个Value记录加入的时间。4. 只能从Head提取，从Tail添加。5. 用作Stack时也可以从Head添加。
				链表结构: (NewStackNode -＞) Head -＞ ... -＞ Tail (-＞ NewQueueNode)。
				第一个用户是Table.GC，延迟删除记录。
				【兼容】
				单向链表原来只发生在自己的Queue内，使用long NodeId指向下一个节点，查询节点时候总是使用自己的Queue.Name和NodeId构造BQueueNodeKey。
				现在为了支持在Queue之间splice，需要使用BQueueNodeKey来指示下一个节点。
				为了兼容旧数据，原来的long类型的变量不能删除，新版需要发现是旧版数据，然后读取并构造出新的BQueueNodeKey。
				1. Root(BQueue)兼容旧数据规则：
				if (Root.HeadNodeKey.Name.isEmpty()) {
					Root.HeadNodeKey = new BQueueNodeKey(ThisQueue.Name, Root.HeadNodeId);
					Root.TailNodeKey = new BQueueNodeKey(ThisQueue.Name, Root.TailNodeId);
				}
				2. Node(BQueueNode) 兼容旧数据规则：
				if (Node.NextNodeKey.Name.isEmpty()) {
					Node.NextNodeKey = new BQueueNodeKey(ThisNode.NodeKey.Name, Node.NextNodeId);
				}
				3. Splice两个Queue时，指向另一个Queue的NodeKey需要先处理好，即已经是新版的结构。现在的代码刚好符合。
*/
public interface BQueueReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BQueue copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    long getHeadNodeId();
    long getTailNodeId();
    long getCount();
    long getLastNodeId();
    long getLoadSerialNo();
    Zeze.Builtin.Collections.Queue.BQueueNodeKey getHeadNodeKey();
    Zeze.Builtin.Collections.Queue.BQueueNodeKey getTailNodeKey();
}
