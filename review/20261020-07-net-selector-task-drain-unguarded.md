# 07 Selector 主循环任务排空无逐任务异常防护

- **严重级**：P2（现实触发需 Error 级逃逸或 OutputBuffer.close 失败，概率低）
- **类别**：事件循环任务排空异常防护不对称
- **子系统**：Net
- **位置**：`ZezeJava/ZezeJava/src/main/java/Zeze/Net/Selector.java:322-323`；对照同文件 `runTask`（:78-84，逐任务捕获 Throwable）；关联 `TcpSocket.java:802-804`（outputBuffer.close() 在 try 外、OnSocketDisposed 覆写的 catch 仅捕获 Exception）
- **状态**：⚔️ 对抗验证 **CONFIRMED 且强化**（2026-10-20 红队四角度全败且发现更强面：taskQueue 主树唯一生产者= TcpSocket.realClose（:802 全仓唯一 .addTask），其任务**不自防护**——:810-819 两个 codec.close() 在 finally 内裸奔，zstd native 释放失败是**Exception 级**在案抛出路径（ZstdFactory.java:358-364 mhFreeCStream 非零→ISE），无需 Error 即穿透。**相邻缺陷**：inputCodecChain.close() 抛出时 outputCodecChain 永不关闭（第二个 native 上下文泄漏）。默认 selectTimeout=0（Selectors.java:33）无限阻塞坐实滞留窗口=该 selector 下次活动前）

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
