# Task

## 简介
这是一个任务系统的框架。应用的时候需要相当的进一步开发，并不是拿来就可以用。
由于任务系统关联很多地方，所以首要的问题怎么进一步开发。

## 整体试图
任务系统(Task)
奖励系统(Reward)
任务配置(TaskGraphics)
地图系统(World)
包裹系统(Bag)
任务条件(Condition)
任务情节(Episode)

## 任务系统(Task)
* 玩家操作（接受任务，完成任务，放弃任务）
* 玩家已接任务列表的访问和状态更新
* npc可接、可交任务提示
* 接受任务时查看任务描述

## 奖励系统进(Reward)
```java
public interface Reward {
	int getRewardId();

	// 下面两个方法的结果通常打包到给客户端的任务结构里面，由客户端完成任务奖励提示的显示。
	// 每种任务奖励的显示方式都可能不一样。
	int getRewardType();
	Binary getRewardParam(long roleId);

	// 发放奖励，一般是任务完成的时候由任务系统调用。广义的，这个方法也可以实现任意时机的奖励发放。
	void reward(long roleId);
}
```
实现自己的各种奖励方式，并注册到RewardConfig中。奖励方式包括但不限于：所有的物品一起奖给玩家；
按权重随机选择部分物品奖励给玩家；根据玩家职业，选择相应的奖品给他；...

奖励除了物品，还可以是任意其他东西，比如Buf，启动一个剧情等等。所以奖励系统本身也是一个关联广泛的系统。

```
taskModule.getRewardConfig.put(new MyReward1());
```

## 任务配置(TaskGraphics)
应用需要一个任务配置编辑器，可以使用TaskGraphics作为任务编辑器的后端存储。由于TaskGraphics是
java开发的，可能不好直接接入。这样的话，应用可以自行决定编辑器的存储，在对接本任务系统时，把配置
数据导入到TaskGraphics中。由于需要对接任务配置，应用自己的任务编辑器不是完全自由的，需要匹配
数据结构。具体要求请查看TaskGraphics的代码。

## 地图系统(World)
* 地图系统需要支持订阅玩家进入退出某个多边形区域的事件；
* 地图系统需要得到新的npc进入玩家视野的列表，用来实现这个npc对玩家的可接任务提示；
注意某些npc可能具有全图（全世界）视野。
* 地图系统需要能得到玩家附近的npc列表，不仅仅是新进入的。

## 包裹系统(Bag)
任务条件是包裹中的物品时，要求能访问包裹系统某个物品数量，能在任务完成的时候删除包裹中一定数量的物品。

## 任务系统的任务条件(Condition)
任务条件和事件的说明
* 任务拥有多个条件，所有条件满足，任务完成。
* 任务条件描述了任务需要达到的目标，事件会触发任务条件状态发生改变。
* 任务条件和事件的匹配有两种模式：1. 按名字进行匹配。2. 每个条件实现定义了自己的事件类，用instanceof进行匹配。

任务条件除了内部预设的一些，应用还可以自行进行扩展。由于任务条件本质上可以是任务的，不仅仅是杀怪拾取物品，
最终会关联到几乎所有的地方。

条件接口如下
```java
public interface Condition extends Serializable {
	String getName();
	boolean accept(ConditionEvent event);
	boolean isDone();

	// 任务完成的时候会调用这个方法，某些条件需要实现这个方法，再次确认isDone。
	// 比如包裹内的物品数量作为条件时，需要实现它。因为任务完成时，物品数量可能发生了变动。
	default boolean finish() {
		return true;
	}

	// 描述，用于客户端显示。
	// 这是个json，用来详细表达ui需要的元素。
	// 最简单的结构是：{ done: "true|false", des: "description" }
	String getDescription();
}
```

框架内预先写好的几个条件实现
* ConditionBag 任务条件是包裹内的某个物品数量达到要求
* ConditionKillMonster 任务条件是杀死某些怪物
* ConditionPickItem 任务条件是拾取某些物品
* ConditionNamedCount 命名计数条件，条件逻辑含义根据自定义名字来决定，只要给他发送该名字的事件，
就会进行计数。达到期望数量后，条件就完成了。
* ConditionCompositeOr 把多个条件组合起来形成或的关系，其中任意一个完成就表示组合完成

## 任务情节(Episode)
```java
TODO
动态情节要能实现任意内容。
class AcceptedTask {
         Bean dynamicContextBean； // 也可能不用bean，而用binary。
}
class DynamicEpisodeTask {
        // 来自任务系统的事件
         void onAccepted(AcceptedTask);
         void onFinish(AcceptedTask);
         void onAbandon(AcceptedTask);
         // 其他这个动态情节需要的任意其他事件，动态情节实现者自己去关联其他系统。比如：
         onMapEvent(enter or leave polygon);
}
动态情节，任务系统只提供很少的事件，还管理了一下动态上下文数据的生命期，
但是完全不管这个上下文的内容。当然动态情节能拿到这个任务相关的其他内容。
```

## 要求的类型及它们引发的其他系统对接要求
* int taskId; 强制要求。
* int NpcId; 强制要求。
* int monsterId; ConditionKillMonster用到了。类型不是int时，只要写个新的条件，不用预设的即可。
* int itemId; ConditionPickItem，ConditionBag用到了。不使用这两个条件就可以接入其他类型Id的物品系统。