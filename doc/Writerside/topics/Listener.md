# Listener

Zeze支持在Table中注册数据变更监听器，当数据发生变化时，回调监听器。不管什么修
改操作，不管有多少修改操作，只要对数据进行了修改，都能得到通知。只要数据定义没有
变化，修改操作随意变化，监听逻辑都保持不变。这个功能非常一般用于同步数据给客户端。
除非逻辑接近事件这种模型，否则不要用监听器实现逻辑。
## 分布式
分布式情况下，Server有多台实例。此时Listener在每个Server上都注册。以后哪台Server
发生了修改，就回调哪一台上的Listener。Listener这个模式不能算是一个良好的分布式定
义，需要注意。
## 接口
```
public interface ChangeListener {
    void OnChanged(Object key, Changes.Record r);
}
```
## 例子
```
public static class ItemsChangeListener implelents ChangeListener {
    void OnChanged(Object key, Changes.Record r) {
        switch (r.getState()) {
        case Changes.Record.Put:
            // 记录整个被替换。
            break;
        case Changes.Record.Edit:
            // 增量变化，
            var notemap2 = (LogMap2<Integer, BItem>)
            c.getVariableLog(tequip.VAR_Items);
            if (null != notemap2) {
                // 访问详细日志。对于这里的LogMap2，note里面包含
                // Replaced(被替换的项)，
                // Removed（被删除的项），
                // Changed（Map中的项是一个Bean并且发生了变化）

                // 由于同步Map的数据时，Changed通常可以合并到Replaced中。
                // 所以首先调用下面的方法合并。
                notemap2.MergeChangedToReplaced();
                // 然后把Replaced，Removed打包到协议中发送给客户端。
            }
            break;
        case Changes.Record.Remove:
            // 记录删除
            break;
		}
	}
}
```
## 注册
```
_tequip.getChangeListenerMap().AddListener(new ItemsChangeListener());
```
## 客户端收到数据变化协议的处理伪码
```
Switch (ItemsChangeNotify.getChangeTag()) {
    Case Put:
        Localmap.clear();
        Localmap.putAll(ItemsChangeNotify.Replaced());
        // Put时Removed没有意义。
        Break;
    Case Edit:
        Localmap.putAll(ItemsChangeNotify.Replaced());
        Localmap.removeAll(ItemsChangeNotify.Removed());
        Break;
    Case Remove:
        Localmap = null;
        Break;
}
```
## 变更日志
1.	Bean的日志LogBean，包含IntHashMap<Log> Variables
2.	List1的日志LogList1，包含List的操作日志。
3.	List2的日志LogList2，包含List的操作日志以及List中的项修改的Changed。
4.	Map1的日志LogMap1，包含Replaced，Removed。
5.	Map2的日志LogMap2，包含Replaced，Removed，Changed。
6.	Set的日志LogSet1，包含Added，Removed。
7.	简单类型（如int）的日志，包含新的值Value。

## 数据同步模式探讨
### 探讨的问题

1.	客户端数据怎么从服务器获取和同步？
2.	客户端获取数据的时机？
3.	客户端数据存储（生命期）探讨？

### 数据获取（侧重和服务器交互）

* Get 模式

客户端主动发送Get请求，服务器返回结果。
【这个模式大大的推荐！】

* Push模式

客户端缓存数据，并且通过交互协议与服务器数据进行同步，客户端数据总是最新的。一般
用于登录期间一直有效的数据。客户端初始Get一次或者0 Get或者订阅方案。服务器主动
通告最新数据给客户端。

* Push 完整数据

当服务器发现数据变更时，把最新数据打包Push给客户端。客户端数据结构和服务器数据
结构不一致，打包时可做翻译。这个模式结构清晰，实现简单。
【这个模式大大的推荐！】

* Push 不完备增量更新

客户端主动Get一次数据，以后服务器Push增量更新给客户端。这里不完备的意思是仅打
包部分修改Log（比如Bean的第一级变量的Map，Set类型相关的修改数据）。
【这个模式限制使用！迫不得已时采用！】
这个模式有两个问题：a）Get和增量更新原子问题。b）增量更新消息丢失。这两个问
题的推荐解决方案：在数据中记录一个修改版本号，修改时递增；Push时带上版本号；客
户端发现版本号出现错误，重新Get再次开启新的增量流程。

* Push 完备增量更新

完备的意思是任何Bean结构的增量更新。在客户端实现这个需要的代码较多。目前只有
conf+cs+net支持（还没测试过）。
【这个模式暂时不推荐使用！】

* Zeze.Listener 的使用场景（限制）

Listener适合同步个人数据。比如角色包裹，服务器监听Listener，发现改变发送push-notify
给该包裹拥有者，客户端接收push-notify并更新本地数据。Listener不适合更新共享的数
据，比如群成员列表这个数据的变化，虽然可以发notify广播，但有点浪费，【实在要使用
也可以考虑这样做】。

### 客户端数据获取时机
* 在Loading界面准备数据，然后一起显示。
* 先显示UI，然后获取数据，再逐渐显示出来。
* 客户端时机点
1.	Auth 验证
2.	Login 登录
3.	Map.EnterWorld 进入地图场景
4.	UI显示时
5.	其他需要数据的时候（对于服务器Auth是必须的）
6.	客户端数据存储管理模式
### 客户端数据存储管理模式
* 全局数据管理器

不够灵活，不太容易满足各种需求。

* 模块自己管理

个人建议这一种。当模块需要热拔插时尤其需要这种。
