# 发布和热更

这一章说明服务器新版发布和热更新的方案。这里的服务器主要指主逻辑服务器。

## 完全重启
停止全部服务器，全部更新，全部重启。更新频率不会太高，重启够快，这个方案最简单，
虽然之后会出现一定的登录高峰，但一般都扛的住。但系统规模越来越大，更新频率可能也
会越来越高，这会导致大量重启，此时就需要考虑其他方式了。这个方案总是可用。在下面
的方案无法使用的时候，总是可以选择它。

## 一台一台更新服务器
服务器本身是多台的，可以一台一台更新并重启。重启服务器上的用户重新登录，只要把重
新登录处理的对用户比较无感，就能实现接近无缝的升级。整个重启的过程可能需要一定的
时间，此时新旧版本同时存在，需要应用具有新旧同时存在的兼容性。

## 基于Instrumentation的任意class热更
Java自带的Instrumentation提供了任意class热更新机制，可以在不重启服务器的基础上，
把新的class转载进去，并保留所有服务器状态，完全无感无缝。这种热更有一定限制，一
般用于紧急BUG修复，临时禁用某些功能，增加日志进行调试等。具体使用限制请查阅
Instrumentation的文档。涉及更新多台服务器时，一般也是一台一台独立进行，虽然可以并
发执行，但在很短的时间内，也是存在新旧版本同时都在运行的时刻，也需要注意更新内容
的新旧版本兼容性。当然一般Instrumentation式热更新，新旧版本问题一般不大。

## 基于ClassLoader的以模块为单位的热更新
服务器不需要重启，按模块为单位热更。一次更新可以包含多个模块。存在新旧版本服务器
同时运行，需要具有这个兼容性，这个也可以提供原子的更新所有服务器来解决。这个热更
新方式对开发模式有要求，并不能随意使用。当服务启用了模块热更新以后，除了模块，服
务还是存在一部分类是不支持热更新的。模块热更新也不是所有模块，可以配置选择哪些模
块支持热更新。模块开发，新版必须兼容旧版，兼容性是一个基本需求。

### 接口化
模块提供的所有服务必须都是接口。模块实现的接口是主接口，如果存在其他接口，必须由
主接口得到。系统装载模块的时候，会登记这个接口，可以由其他模块查询并使用。接口中
的方法可以使用所有的不是模块内定义的类型，包括java基本类型，jdk容器，第三方库，
zeze等。模块内定义的类如果需要公开也必须接口化。Bean也是模块内自定义的类型，也
是不能用于接口方法的参数的，这个有计划解决，但短期内不支持Bean作为接口参数了。

### 接口引用保存规则
原则上不保存任何模块的提供的服务的接口引用。模块主接口特殊处理，引用其他模块主接
口时，可以按下面方式保存一个上下文，避免每次都需要查询。
```
IModuleSome getSomeService() {
　　if (this.moduleSome == null）
　　this.moduleSome = HotManager.get(“MySol.ModuleSome”, IModuleSome.class);
　　return moduleSome.getService();
}
```

### 新版接口兼容性
新版接口必须兼容旧版接口，旧版本接口一旦发布就不能再修改。例，IModuleSome2
extends IModuleSome。旧版接口一旦发布就不能再修改。这点很重要，就在这里再重复一
次了。

### 无状态模块
模块除了使用zeze的table等服务，没有任何自定义数据，即状态。除了没有定义自己的
变量，还需要注意 java.ScheduleThreadPool.schedule调用也是程序状态。对于schedule，
如果它在stop被停止，在start时重新启动，那么这个状态相当于被自动恢复了，此时仍然
可以看作“无状态“，算半无状态吧。Timer问题下面会继续说一些。Zeze的Table在模块更
新的时候，状态会被自动刷新。其他服务有时候也是有状态的，可能需要使用特别的版本。
见后面Zeze服务限制。Zeze的服务在热更下都会提供解决方案，而除了Zeze服务，程序
没有自己状态，就满足了“无状态“这个条件。这种情况下的热更，开发除了上面几点，如兼
容性等要求外，不需要更多额外的支持，就可以轻松的支持模块热更了。

### Timer的使用模式
这里的Timer特指Zeze.Component.Timer。如果是java自带的线程池的Timer，可以参考
这里说的，自行判断处理方式。
* start，stop模式

热更模块在start的时候注册的Timer，在stop的时候注销Timer。这种情况下不需要
对timer进行额外处理。

* 继承模式

当Timer的注册是跟随逻辑走的，或者想保持一次注册，以后延续固定的节奏，那么就
不要重新注册，而是在upgrade的时候从旧接口把已经注册的timerId得到，保存到新
模块实例内。继承模式在注册时需要判断模块是第一次启动还是处于热更中。下面的
辅助函数能判断区分这种情况，而且这个方法在没有启用热更时也能工作。
```
private boolean isHotUpgrading() {
    var hotManager = App.Zeze.getHotManager();
　　 if (null == hotManager)
　　 return false;
    return hotManager.isUpgrading();
}

void start() {
    Zeze.newProcedure(() -> {
    If (!isHotUpgrading())
        timerInherit = Zeze.getTimer().schedule(…); // 第一次启动注册。
        return 0;
　　}, “register timer”).call();
}

void stop() {
    Zeze.newProcedure(() -> {
    If (!isHotUpgrading())
        Zeze.getTimer().cancel(timerInherit); // 程序退出，不是热更中。
        Return 0;
    }, “unregister timer”).call();
}

void upgrade(HotService old) {
    Var iMyHotService = (IMyHotService)old;
    timerInherit = iMyHotService.getTimerInherit(); // 继承过来。
}
```

### Start，Stop实现要求
Start，Stop需要允许反复执行。热更的启动停止不能破坏状态。即一个模块被Stop之后再
次Start，需要能继续正常执行。也就是说Stop不能破坏模块状态。在这里上一节中的
isHotUpgrading就有用了。它可以用来区分第一次启动和最后一次关闭以及中间热更的启动
停止。这个特性是热更用来处理错误用的，热更过程中首先会Stop模块，当发生错误时，
热更过程会重新Start模块恢复执行，同时中断热更的安装。Start实现过程必须是都是同步
调用，不能等待其他线程。因为热更过程中，Start此时处于写锁内，启用新的线程会造成
死锁。

### 有状态模块
模块具有状态，模块更新的时候就需要处理处理状态迁移和刷新问题。这里分为两种。
* 模块状态是自己内部创建的变量。

热更的时候，系统调用HotService 的方法：Void upgrade(HotService old);通知模块
进行数据迁移。由于old是接口，所以需要支持迁移的模块还需要在接口中定义访
问旧状态的方法，（TODO，这是个问题，需要想想）。完全无缝迁移对于某些复杂
模块，或者当修改比较大时，是比较困难的。这时应用上可以采取让玩家重新登录
的方式，避免热更新的时候去迁移状态。举个例子，游戏的地图服务器，可选的保
存一些状态（所在地图位置方向），然后把玩家踢下线，玩家重新登录时恢复这些状
态。

* 模块状态保存了来自其他模块得到的数据引用。

分析这个问题比较复杂，定义一个refresh接口是远远不够的。涉及两个基本问题。
A)使用了哪些模块？怎么通知？B)数据引用一般随着逻辑功能执行的过程保存下来
的，这个刷新看来是不可能的任务。推荐的规则是禁止保存来自其他模块的数据。
这能确保没有这个问题，但对应用限制比较大。还有个办法是对Bean的发布和热更
进行特殊处理，使得Bean可以在接口中使用，并且作为返回值时，也可以保存下来。

### Redirect接口方法的参数和结果规范
Redirect接口方法直接公开到接口中，参数结果部分在解决Bean作为参数之前，和普通方
法一样也是不能使用Bean作为参数的。但Redirect不能返回结构结果，对它的功能影响较
大。在Bean能作为参数之前，提供一个临时的解决思路。当Redirect结果是一个Bean时，
把这个Bean.class发布到非热更部分，不进行热更。当Bean结构需要修改时，定义新的Bean，
并应用到新的Redirect方法接口中。旧的方法和Bean也保留。这种方式虽然有一点点麻烦，
但能工作。当前状态是如果模块有Redirect直接不支持热更。
* RedirectAllFuture&lt;MyRedirectResult&gt; 在xml种定义一个特殊的Bean，如下：
&lt;bean name="MyRedirectResult" RedirectResult="true"/&gt;
* 其他参数或者结果，如果使用了自定义结构，只能是Bean或Data或BeanKey。
这些自定义结构按它们原来方式定义即可。

### Zeze服务限制
启用了模块热更新支持以后，Zeze服务需要一定限制。
```
A)	只能由应用的非热更代码调用，Zeze会阻止违规调用。
	Zeze.Component.TimerAccount & Zeze.Component.TimerRole
　　scheduleOnline(… TimerHandle handle …) 
　　scheduleOnlineNamed(… TimerHandle handle …)
　　所有以”TimerHandle handle”形式提供回调的在线定时器都会进行限制。
	Zeze.Util.EventDispatcher，用于Zeze.Game.Online & Zeze.Arch.Online
add(mode, handle) 调用者必须来自非热更模块。
B)	热更新模块必须使用接口。
	Zeze.Component.TimerAccount & Zeze.Component.TimerRole
scheduleOnlineHot(… Class<? extends TimerHandle> handleClass …)
scheduleOnlineNamedHot(… Class<? extends TimerHandle> handleClass …)
	Zeze.Util.EventDispatcher，用于Zeze.Game.Online & Zeze.Arch.Online
addHot(mode, handleClass) 热更模块必须使用这个接口注册事件。
```

### 模块热更新启用配置
* project hot=true 需要启用模块热更新时，首先必须配置这个，默认时false。
* module hot=true 每个需要热更新的模块都需要单独配置，默认是false。

### 在接口中使用自定义Bean的方案（TODO）
打包模块的时候，Bean打包到”非热更”模块；
Bean的热更采用基于字节码热更（如，sprint-loaded）的方案；
然后Bean就可以在接口中使用了。

### 直接暴露数据表（TODO）
在接口中使用自定义Bean实现前不能直接暴露数据表。
暴露的表不能把引用保存下来，只能临时使用。
