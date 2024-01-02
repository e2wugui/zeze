# Task

## 任务系统目标
【通用】任务系统。通用要求相当的抽象和灵活性。
基本目标：实现基本的灵活的核心。
扩展目标：实现一些比较常见的任务条件。
开发扩展：任务系统程序员可以自定义任务条件进行扩展。

## 任务系统基本需求
玩家当前任务管理，包括客户端当前任务列表通告。
任务配置是有向图，不允许出现环。
任务交接（Npc头上叹号）相关状态和提示。	

## 任务系统相关系统
1. Reward 奖励
2. Achievement 成就
3. Statistics 未纳入成就的其他统计。

## 任务系统核心抽象
1. Task 任务，包含多个Phase
2. Phase 阶段，包含多个条件，每一个阶段条件完成才进入（显示）下一个阶段，是控制条件的完成顺序，和对很多条件的任务进行分类管理。
3. Condition 条件，核心抽象
4. ConditionEvent 条件事件（比如杀了一个怪，获得物品等等），发送给Task进行派发处理，最终Condition接受自己匹配的事件并更新自己的状态。

## 任务配置（TaskGraphics）
任务配置由有向图描述。存在多个根。任务前置类型：All 所有的前置任务必须完成；AnyN 前置任务中可选的完成N个。
用一个参数（PreposeRequired）描述，0表示all，[1, n]表示AnyN当PreposeRequired为n时，含义和0一样，都表示all了。
但是为0时，如果前置任务数量发生了变化，仍然表示all。PreposeRequired>n一般是配置错误。这个错误合适的处理方式
是允许这样的配置。因为前置任务数量变少时，可能PreposeRequired没有修改，应该尽可能运行下去。

## 任务奖励
在一般任务系统中，任务奖励除了奖励物品金钱经验，还可以奖励buf，一个特效等等。
最终任务系统关联非常多其他的系统，不符合我们设计目标。所以，在这个任务方案中，
奖励仅仅用一个奖励编号描述，所有的奖励细节由奖励系统实现。这里设想一下奖励系统的功能，
* 奖励金钱经验物品。
* 奖励一个物品列表中的随机物品。
* 玩家获得一个buf。
* 根据玩家职业，从他的职业装备列表中随机选择一个装备。
奖励系统的实现是每个奖励配置异构，可以抽象出一个具有reward(long roleId, int id)接口。
由奖励系统去关联其他的各种系统，解耦任务等其他任意需要奖励的地方。
比如杀怪掉落表也可以纳入奖励系统。
【结论：任务系统不做奖励啦。由Reword解决】但需要注意下面的问题。
1. 但是奖励系统预计要给任务面板奖励的说明提供足够好用的接口，因为有一些任务玩家需要接任务阶段就看到奖励。
2. 假如玩家系列任务中需要属于他自己的任务npc（交接任务的npc，可能地图上生成但不广播给其他玩家；跟随的npc，需要广播给其他玩家），是通过奖励系统生成么，他销毁通过什么控制
这是一个【好问题】任务进行过程中产生的动态内容是个好问题。通用任务系统需要参与这个。需要看看怎么设计。

## 任务可接提示（Npc头顶叹号）
### 需要数据：
a) 玩家已完成任务历史记录，TaskCompleted。
b）任务配置（TaskGraphics）。
c) npc可接任务索引，NpcAcceptTask。任务配置中定义了接任务npc和交任务npc等信息，需要为npc建立索引。

// 是否需要支持低等级任务不显示叹号的功能，玩家对话可以接，但大地图上不提示
// 还有是否需要支持可接任务列表或者推荐任务列表这个界面，这种周围没有可见npc，没办法通过输入npc来检查，这种是策划另外编辑，还是任务系统自己来实现
// XXX 低等级不显示叹号，等等显示控制逻辑目前考虑不提供完整接口，就像下面的接口一样，只是返回任务和状态，外面根据细节（比如等级）自行决定更多的显示控制。
// XXX 【暂定】可接任务列表或者推荐任务列表，进一步开发的程序员可以通过特殊npcid使用下面的接口，然后在接任务时，不检测这种npcid和玩家的距离。

## 任务可交提示（Npc头顶黄色问号）
需要数据：玩家已接任务列表。

## 任务接受触发
a) npc 对话接受
b) 到达地图某个位置自动接受，核心是任务需要配置到地图中。由地图服务器触发接受任务（或者地图服务器提供位置监控能力）。
   这种任务接受了，在持续期间一直存在，但是玩家离开任务范围可能不显示，回到范围内又重新显示，所以实际上需要保存很久，需要用时间控制。
c) 其他事件触发。
   配置到任意其他地方。
【总结】任务接受对于任务系统来说就是添加任务到玩家已接任务列表。
当提供了接受任务的接口后，任何地方都可以调用。
// d) 上个任务完成的时候，直接接到新的任务在身上
// 玩家已接任务是否有上限，到达上限后，事件触发的任务能否接到玩家身上
// XXX 【d)】可以作为功能实现进去，如果接新任务npc配的很远也行，此时需要忽略npc和玩家的距离判断。
// XXX 【这个需要考虑】任务上限应该进限制玩家主动接的任务，系统自动触发的原则不做限制。实际上这种需要通过整个系统本身的设计来控制，
// XXX 比如剧情触发的任务是有限的，世界位置触发的任务可能需要有一个小的独立限制，顶掉最老的什么的。

## 任务情节
上一节提到的任务相关动态内容。动态内容本质上是任意的，需要抽象出来由程序员进一步开发。

## 其他任务功能（需要权衡是否做入这个抽象的核心的实现）
a) 自动完成
b) 任务有持续时间

## TaskCompleted 存储草稿
这个是角色数据，每个角色记录一份。
可以使用LinkedMap存储，即不限容量，也符合按任务编号快速查询的能力。
【警告】需要考虑这个数据的其他快速访问需求，可能需要建立自己的数据结构。

## TaskGraphics 存储草稿
这个是配置数据，定义好核心系统需要的数据结构和用户可自定义数据结构以后。
提供构造数据的接口，由使用者从自己的任务配置数据中构造出这个系统需要的数据。
草稿：
class TaskGraphics
{
	Map<TaskId, Task> TaskNodes; // 保存所有的任务配置。
	Set<TaskId> Root;
}

class Task
{
	Set<TaskId> PreposeTasks;
	Set<TaskId> FollowTasks;
	int AcceptNpc; // 接受Npc
	int FinishNpc; // 交接Npc
	...
	dynamic CustomData; // 用户自定义扩展数据。
	【整理核心，数据是重中之重】
}

// 没看到玩家已接任务存盘数据结构
// 没看到任务接受条件怎么检查的
// XXX 已接任务存盘结构就是下面的class Task等。
// XXX 任务检查条件，检测通用目标已经定义好的条件，其他的通过自定义接口由进一步开发人员实现。存储上，TaskConfig用zeze.dynamic实现任意自定义数据的能力。

## 任务系统总体结构

```
public class TaskBase {
	// from user
	void accept(int taskId);
	void finish(int taskId);
	void abandon(int taskId);

	// 玩家已接任务列表管理接口，涉及读取以及状态变化更新通知。
	// TODO 更新采取的Listener模式：全部增量更新可实现（目前部分客户端环境不支持），但是这种更新方式会失去当前变化的上下文。
	...

	// 服务器内部接口
	void accept(long roleId, int taskId);
	void abandon(long roleId, int taskId);
	void dispatch(long roleId, ConditionEvent event);
}

public class RewardBase {
	Reward getReward(int rewardId); // 唯一接口
	// Reward 的抽象关键：任务需要读取奖励内容并表现给玩家。
	// Reward=list<(type, itemId)> 是一个物品列表，这里的物品是广义的物品，实际上可以包括任何东西。
	// 客户端拿到这个列表自己根据type，itemId进行表现。
	// TODO 奖励全部包含，选择部分，等等这个怎么抽象。

	public abstract class Reward {
		abstract list<(type, itemId)> getReward();
		abstract void reward(long roleId);
	}
	// Reward 实现
	// 提供一个默认实现？
	// 解决了上面的TODO，也就解决了奖励的配置接口。
}

public class AchievementBase {
	void onTaskCompleted();
	// 实现接口肯定非常杂。
	// 这个系统本身也有点像任务系统，由有向无环图结构描述，节点是成就Condition。
	// 这个系统是未来需要实现的，它和任务系统的交互表现为程序员订阅任务系统的事件，自行调用。
	// 不直接耦合。
	// 【结论，未来实现这个系统】
}

public class Map {
	// 需要的 Map 的接口，抽象待定。
	void register(polygon, handle); // 订阅进入离开某个多边形区域的事件。用来实现世界任务。
}

```

## 任务实现草稿
```
Condition.Name使用string。
class Role {
	List<Task> tasks; // 已接（即当前）任务列表。
}

class Task {
	List<Phase> phases; // first is current phase.

	// 当条件发生了变化，调用这个更新任务状态。
	public boolean accept(ConditionEvent cevent) {
		var currentPhase = phases[0];
		var accecpted = currentPhase.accept(cevent);
		if (accecpted) {
			if (currentPhase.isDone()) {
				// 可能不删除阶段数据，用currentPhaseIndex来记录。看设计目标。暂定删除。
				phases.remove(0);
				if (phases.isEmpty()) // task is done.
					update task state;
			}
			// ...
			update view;					
		}
		return accecpted;
	}

	public void addPhase(Phase phase) {
		phases.add(phase);
	}
}

class Phase
{
	List<Condition> conditions; // key is condition name

	public boolean accept(ConditionEvent cevent) {
		var accepted = false;
		for (var condition : conditions) {
			if (condition.getName().equals(cevent.getName())
				&& match.accept(cevent)) {
				if (match.isDone()) {
					conditions.remove(cevent.getName());
				}
				accepted = true;
				if (cevent.isBreakIfAccepted())
					break;
			}
		}
		return accepted;
	}

	public boolean isDone() {
		return conditions.isEmpty();
	}

	public void addCondition(Condition condition) {
		conditions.add(condition);
	}
}

// 核心抽象，需要把各种需求拿来过一遍，审核整体设计。
abstract class Condition
{
	// 条件名字。
	public abstract String getName();

	// 接受条件事件。
	// return true 事件接受，false 事件拒绝。
	public abstract boolean accept(ConditionEvent cevent);

	// 条件是否完成。
	public abstract boolean isDone();

	// 任务完成的时候调用，通常情况下，不需要额外实现。需要实现的例子参见后面的ConditionBag。
	public boolean finish() {
		return true;
	}
}

abstract class ConditionEvent {
	private boolean breakIfAccepted = false;

	public ConditionEvent(boolean breakIfAccepted) {
		this.breakIfAccepted = breakIfAccepted;
	}

	public ConditionEvent() {
		this(false);
	}

	// 事件名字。一般和相对应的Condition名字一样。
	public abstract String getName();

	public final boolean isBreakIfAccepted() {
		return breakIfAccepted;
	}
}

// 通用计数型条件，拥有一个计数值和期望值，给他不同的名字就能表示所有这种计数方式的条件。
public class ConditionNamedCount extends Condition {
	private String name;
	private int count;
	private int expected;

	public ConditionNamedCount(String name, int expected) {
		this.name = name;
		this.expected = expected;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean accept(ConditionEvent cevent) {
		if (cevent is Event e) {
			count += 1;
			return true;
		}
		return false;
	}

	@Override
	public abstract boolean isDone() {
		return killed >= expected;
	}

	public static class Event extends ConditionEvent {
		private String name;

		public Event(String name, boolean breakIfAccepted) {
			super(breakIfAccepted);
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}
	}
}

// 杀怪计数型条件，可以配置可接受的怪物Id。怪物Id之间是或者的关系。如，杀死兔子或乌龟 0/10
class ConditionKillMonster extends Condition {
	private int count;
	private int expected;
	private List<Integer> monsters; // 本来是期望的怪物Id集合，但为了描述顺序使用List，一般是少量的，所以问题不大。

	public ConditionKillMonster(int expected, List<Integer> monsters) {
		this.expected = expected;
		this.monsters.addAll(monsters); // copy
	}

	@Override
	public boolean accept(ConditionEvent cevent) {
		if (cevent is Event e) {
			if (monsters.contains(e.monster)) {
				count += e.increaseNumber;
				return true;
			}
		}
		return false;
	}

	@Override
	public abstract boolean isDone() {
		return killed >= expected;
	}

	@Override
	public String getName() {
		// 这种条件是模板性质的，它实际被配置成什么条件由内部的参数决定。
		return ConditionMonsterKill.class.getName();
	}

	// 杀死怪物的时候触发事件。
	public static class Event extends ConditionEvent {
		private int monster;
		private int increaseNumber;

		public Event(int monster, int increaseNumber) {
			this.monster = monster;
			this.increaseNumber = increaseNumber;
		}

		@Override
		public String getName() {
			return ConditionMonsterKill.class.getName();
		}
	}
}

class ConditionCompositeOr extends Condition {
	public Map<String, Condition> composite;
}

class ConditionBag extends Condition {
	private int count;
	private int expected;
	private int item; // 不考虑接受多个物品。这也可实现，不过即使实现也不建议用。

	@Override
	public boolean accept(ConditionEvent cevent) {
		if (cevent is Event e) {
			if (item == e.item) {
				var countNow = bag.count(e.item);
				if (countNow != count) {
					count = countNow;
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public abstract boolean isDone() {
		return count >= expected;
	}

	@Override
	public String getName() {
		return ConditionBag.class.getName();
	}

	@Override
	public boolean finish() {
		if (bag.remove(item, expected))
			return true;
		// 当发生完成任务的时候扣除物品失败，意味着物品变成了不够的，更新一下本地的计数。
		// 最外面完成任务的时候会检查finish结果，失败的时候，会重新更新任务的状态。
		count = bag.count(item);
		return false;
	}

	// 在包裹中添加或者删除物品的时候触发事件。
	public static class Event extends ConditionEvent {
		private int item;

		public Event(int item) {
			this.item = item;
		}

		@Override
		public String getName() {
			return ConditionBag.class.getName();
		}
	}
}

也许一开始的定义是对的，Condition.Name ConditionEvent.Name就是逻辑上需要命名的名字。
写好一个模板（比如ConditionExpectedCount）后，通过命名得到这种计数条件，每个命名一个
具体类型。这个必须是应用上能区分开的。当像ConditionMonsterKill这种模板的写法时，它的
名字就是模板名字，具体是哪种Condition，通过那内部的数据描述出来，如，小兔子或小乌龟 0/10，
其中小兔子或小乌龟是ConditionMonsterKill内部的acceptedMonsters构造出来的。总结：也就是说
Condition的实现可以一个名字代表一种具体的条件，也可以实现成模板，通过内部参数构造出具体
条件。完美了？

// 例子
public class ConditionFactory {
	// 这个例子用下面的条件（ConditionKillMonster）也可以实现。
	public Condition newKillMonster1(int expected, int monsterId) {
		return new ConditionNamedCount(ConditionNamedCount.class.getName() + ".Monster." + monsterId, expected, "打败" + Monster.getName(monsterId));
	}

	public Condition newKillMonsterOr(int expected, int monsterId ...) {
		return new ConditionKillMonster(expected, List.of(monsterId), String.format("打败%s或%s", monsterId, monsterId)); // 外面格式化？这个格式化乱写的。
	}
}
```
### 任务可接提示算法描述：
```
List<TaskWithAcceptState> CheckNpcExclamationMark(role, npc)
{
	var result;
	foreach (var task : npc.NpcAcceptTask)
	{
		if (CheckPreposeTask(task, role.TaskCompleted)
			&& CheckTaskAcceptCondition(task, role) // 需要抽象？？？
			)
		{
			result.Add(task, eAcceptable); // 这个任务可接（黄色叹号）。
		}
		else
		{
			result.Add(task, eHasTask); // 有任务，但还不能接（灰色叹号）。
			// ？？？正常情况灰色叹号是不显示的，只有特殊（比如剧情任务）情况下，需要显示。
			// 这个怎么提供自定义选项？？？目前考虑是给出结果，由外面决定是否显示灰色叹号。
		}
	}
}

bool CheckPreposeTask(task, completed)
{
	var n = task.PreposeRequired;
	if (n <= 0 || n > task.PreposeTask.size())
		n = task.PreposeTask.size();

	foreach (var prepose : task.PreposeTask)
	{
		if (completed.contains(prepose))
		{
			--n;
			if (n == 0)
				return true;
		}
	}
	return false;
}
【注意】上面算法中的task变量指的的是任务配置中的任务。
```

### 任务可交提示算法
```
List<Task> CheckNpcQuestionMark(role, npc)
{
	var result;
	foreach (var task : role.TaskList)
	{
		var config = TaskGraphics.GetTask(task.Id);
		if (config.FinishNpc == npc && task.IsDone())
		{
			result.Add(task);
		}
	}
	return result;
}
```
