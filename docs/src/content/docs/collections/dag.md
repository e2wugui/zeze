---
title: "DAG 有向无环图"
sidebar:
  order: 7
---

`DAG` 是 Zeze 提供的**持久化有向无环图**集合，用于管理节点之间的依赖关系。它基于 JGraphT 的 `DirectedAcyclicGraph` 实现，支持添加节点、边以及环检测。所有操作在事务中执行，数据自动持久化。

## 包路径

```
Zeze.Collections.DAG<V extends Bean>
```

## 快速开始

```java
Zeze.Application zeze = new Zeze.Application(config);
DAG.Module dagModule = new DAG.Module(zeze);

// 打开 DAG 实例
DAG<TaskNode> taskGraph = dagModule.open("build_deps", TaskNode.class);
```

## API 参考

### Module 方法

| 方法 | 说明 |
|------|------|
| `open(String dagName, Class<BNodeType> nodeType)` | 打开 DAG 实例 |

### DAG 操作

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `addNode(long id, V value)` | `void` | 添加节点 |
| `addEdge(long from, long to)` | `void` | 添加有向边（from -> to） |
| `checkValid()` | `void` | 校验 DAG 合法性（无环），不合法则抛异常 |
| `isEmpty()` | `boolean` | 图是否为空 |
| `getName()` | `String` | 获取名称 |

## 使用示例

### 任务依赖管理

```java
DAG<TaskNode> taskGraph = dagModule.open("build_deps", TaskNode.class);

zeze.newProcedure(() -> {
    // 添加任务节点
    TaskNode compile = new TaskNode();
    compile.name = "compile";
    taskGraph.addNode(1, compile);

    TaskNode test = new TaskNode();
    test.name = "test";
    taskGraph.addNode(2, test);

    TaskNode deploy = new TaskNode();
    deploy.name = "deploy";
    taskGraph.addNode(3, deploy);

    // 建立依赖关系：compile -> test -> deploy
    taskGraph.addEdge(1, 2); // compile -> test
    taskGraph.addEdge(2, 3); // test -> deploy

    // 校验合法性（检测是否有环）
    taskGraph.checkValid();

    return 0;
}, "dag_ops").call();
```

## 内部实现

| 存储表 | 键 | 值 | 用途 |
|--------|----|----|------|
| `_tDAGs` | name | BDAG 根信息 | DAG 元数据 |
| `_tNode` | name + nodeId | BDAGNode | 节点数据 |
| `_tEdge` | name + edgeId | BEdge | 边数据 |

内存中使用 JGraphT 的 `DirectedAcyclicGraph<BDAGNodeKey, DefaultEdge>` 维护图结构，`addEdge` 时由 JGraphT 自动检测环。

## 注意事项

1. **环检测** -- 添加边时如果产生环，`checkValid()` 会抛出异常
2. **名称限制** -- 名称不能为空
3. **实验性** -- DAG 模块的 `isValid` 方法仍在完善中（当前始终返回 `true`），生产使用需注意
4. **依赖 JGraphT** -- 运行时需要 JGraphT 库
