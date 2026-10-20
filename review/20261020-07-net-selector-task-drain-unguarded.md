# 07 Selector 主循环任务排空无逐任务异常防护

- **严重级**：P2（现实触发需 Error 级逃逸或 OutputBuffer.close 失败，概率低）
- **类别**：事件循环任务排空异常防护不对称
- **子系统**：Net
- **位置**：`ZezeJava/ZezeJava/src/main/java/Zeze/Net/Selector.java:322-323`；对照同文件 `runTask`（:78-84，逐任务捕获 Throwable）；关联 `TcpSocket.java:802-804`（outputBuffer.close() 在 try 外、OnSocketDisposed 覆写的 catch 仅捕获 Exception）
- **状态**：⚔️ 对抗验证 CONFIRMED → ✅ **已修复（2026-10-20）**：主循环排空改用 runTask（逐任务 catch-Throwable 记日志，对齐 drainClosedTasks 路径，单任务异常不再中断本轮排空）；顺带修相邻缺陷——TcpSocket.realClose finally 内 input/output 两个 codec.close() 各自独立防护（此前 input 抛出令 output 永不关闭，zstd 第二个 native 上下文泄漏；ZstdFactory mhFreeCStream 非零→ISE 是 Exception 级在案抛出路径）。验证：javac 零告警；行为验证由车道承接（网络 dispose 竞态无法离线确定性复现）

## 缺陷

Selector.run() 主循环的任务排空是裸循环，无逐任务 try/catch——一个任务抛出（Error 级逃逸或
dispose 链异常）会中断本轮排空，剩余任务滞留队列。滞留任务延迟到该 selector 的下一次唤醒才
执行；安静服务器的后续静默期可无限期延迟（如其他连接的 realClose dispose 任务：在飞 Rpc
立即失败、zstd native 上下文 close、buffer 池归还）。同文件 `drainClosedTasks` 路径的
`runTask` 逐任务捕获 `Throwable`——防护不对称是结构性遗漏。

## 代码证据

```java
// Selector.java:322-326
for (Runnable r; (r = taskQueue.poll()) != null; )
    r.run();                    // ← 无 try/catch；runTask 同职责却有逐任务 Throwable 防护
} catch (Throwable e) {         // 外层只保证 while 存活
    logger.error("Selector.run exception:", e);
}
```

## 触发场景

T1(selector 线程) poll 出 dispose 任务 → `outputBuffer.close()` 抛 Error/Throwable 从
`Service.OnSocketDisposed` 覆写逃逸（TcpSocket.java:802-804 该调用在 try 外、catch 仅
Exception）→ 异常穿出排空循环 → 外层记日志 → `selector.select()` 阻塞 → T2 同 selector 上
其他连接的 dispose 任务滞留。

## 修法方向

主循环排空复用 `runTask`（或等价逐任务 catch-Throwable-记日志），一行级修复。
