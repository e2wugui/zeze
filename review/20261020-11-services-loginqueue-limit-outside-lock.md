# 11 LoginQueue 上限检查在 allocateLock 外 —— 【已撤回：误报】

- **状态**：❌ 误报，2026-10-20 对抗验证红队击杀，主审亲验关键链后撤回
- **位置**：`Services/LoginQueue.java:225-244`

## 撤回理由（accept 严格单线程，竞态前提不可达）

原判称"多个 selector 线程并发 accept 时队列可超 maxConnections"。红队击杀链（主审逐环亲验）：

1. **selector 池默认 1 线程**：`Selectors.java:20-22` InstanceHolder 单例 `new Selectors("Selector")`
   → 构造器 `this(name, 1, null)`（:67-69）count=1；`Service.setSelectors` 全 main 零调用方；
   `new Selectors(...,count≥2)` 仅测试 fixture（TestTcpSocketDisposeAfterSelectorsClose:29 等）
   且从不挂 LoginQueueService；无任何 XML/config 旋钮可调池大小（grep SelectorCount 零命中）。
2. **每 listener 单 key 单 selector**：`TcpSocket.java:182` `selector = service.getSelectors().choice()`
   + `:186 register`——监听 socket 恰好注册在一个 selector 上。
3. **OnSocketAccept 同步回调链**：accepted 套接字构造器（`TcpSocket.java:279`）在**该 selector
   线程上同步**调 `service.OnSocketAccept(this)`；`tryOnAccept` 全库唯一调用方是
   `LoginQueue.java:39` 的覆写。
4. **并发方向排除**：tryOnAccept 与 drainQueue（DaemonTimer 线程）/上报线程在 allocateLock 上
   串行，且并发方对 queue **只删不加**（queue.add 全文件仅 :239 一处）——删除只会让锁外检查
   偏保守（错发一次 PutQueueFull，下个连接自愈），数学上不可能造成超限。

结论：check-then-add 之间不存在另一个 tryOnAccept 与之交错，发现主张的触发条件在现行代码库
不可表达、不可部署、不可触达。

## 残留备注（不构成缺陷，供参考）

- 若未来引入多线程 selector 池（API 支持 Selectors.add/setSelectors），该 check-then-act 将按
  原判量级越限——检查搬进 allocateLock 是一行级前瞻加固。
- `maxConnections` 语义错位可另行立案：ServiceConf.java:39 定义为 Service 级在线连接上限
  （Service.java:485 对 socketMap.size() 检查、注释自认"尽力而为"），LoginQueue:226 把它复用
  为队列长度上限。

## 原始发现（仅存档）

原判称检查在锁外致并发越限入队（有界超额）——前提（多线程 accept）不成立，见上。
