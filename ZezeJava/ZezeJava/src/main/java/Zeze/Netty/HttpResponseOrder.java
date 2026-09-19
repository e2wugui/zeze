package Zeze.Netty;

import java.util.ArrayDeque;
import io.netty.channel.ChannelPromise;
import io.netty.util.collection.IntObjectHashMap;
import org.jetbrains.annotations.NotNull;

// pipelining响应序化器的状态类型（从HttpExchange拆出，仅EventLoop单写者线程访问）。
// 机制与不变式见HttpExchange的序化区注释；方法（登记/写/让位/推进/janitor）仍在HttpExchange上
// ——它们与exchange字段交织，类型外提只为削减HttpExchange体积。

// 序化器per-channel（channel属性）。全部字段仅channel的EventLoop线程访问（单写者）。
final class ResponseSequencer {
	int nextOrderId = 1; // 下一个到达序（registerResponseOrder分配）
	int writingOrderId = 1; // 当前持笔orderId：此前的已全部写出出队
	final IntObjectHashMap<OrderEntry> entries = new IntObjectHashMap<>(); // 在途entry
}

// 在途exchange的序化条目（仅EventLoop线程访问）。
final class OrderEntry {
	final HttpExchange x;
	ArrayDeque<DeferredWrite> pending; // 未轮到时的挂起响应写，懒建
	boolean finished; // exchange已close/endStream（FINISH族挂起写仍按序送出）
	boolean started; // 本entry的按序写已开始（出站tripwire的判定依据）

	OrderEntry(HttpExchange x) {
		this.x = x;
	}
}

/**
 * @param promise 调用方给定的promise（可为voidPromise），提交时原样传递
 */
record DeferredWrite(@NotNull Object msg, @NotNull ChannelPromise promise, boolean flush) {
}
