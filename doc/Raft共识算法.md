# Raft共识算法

## 状态

###### 每个节点的持久化状态 (在回复RPC前都要确保更新到可靠的存储上)
* `currentTerm`: 自己已知的最新term(初始为0,每次+1增量)
* `votedFor`: 为当前term投票的节点ID(初始及没有时为null)
* `log[]`: log序列(初始为空), 每条log包含可在状态机上执行命令和从leader接收的term值(从1开始)

###### 每个节点的可变状态
* `commitIndex`: 当前最大的已经提交的log索引(初始为0,每次+1增量)
* `lastApplied`: 当前最大的已经执行的log索引(初始为0,每次+1增量)
* 自己的身份(follower/candidate/leader, 初始为follower)
* 选举超时时间

###### 仅leader用的可变状态 (每次选举成功时重置)
* `nextIndex[]`: 各节点下一个要发送的log索引(初始化为leader最新log索引+1)
* `matchIndex[]`: 各节点最大已被复制的log索引(初始为0,每次+1增量)

## 协议

#### RequestVote RPC (向其它节点请求给自己即将成为leader投票)
###### 请求字段
* `term`: 自己的新term
* `candidateId`: 自己的节点ID
* `lastLogIndex`: 自己的log[]中的最新log的索引
* `lastLogTerm`: 自己的log[]中的最新log的term
###### 回复字段
* `term`: 处理者的`currentTerm`, 用于给发起者更新
* `voteGranted`: 是否给发起者投票成功
###### 处理请求流程
* 如果`term < currentTerm`: 投票失败
* 如果`votedFor`为空或等于请求的`candidateId`, 且请求的`lastLogIndex`和`lastLogTerm`都不小于自身的对应值:
  * 更新`votedFor`, 重置选举超时(在固定时间基础上略微随机调节), 并回复投票成功
  * 否则投票失败

#### AppendEntries RPC (只由leader发起, 用于复制log给其它节点,也用于心跳)
###### 请求字段:
* `term`: 自己的term
* `leaderId`: 自己的节点ID, 用于让follower知道并通知客户端当前的leader
* `prevLogIndex`: 发送log的前一个log的索引
* `prevLogTerm`: 发送log的前一个log的term
* `entries[]`: 发送的log列表, 空表示心跳
* `leaderCommit`: 自己当前的commitIndex
###### 回复字段:
* `term`: 处理者的currentTerm, 用于给leader更新
* `success`: 回复是否成功, 表示处理者是否成功复制了log
###### 处理请求流程:
* 如果`term < currentTerm`: 回复失败
* 如果没有`prevLogIndex`和`prevLogTerm`的log: 回复失败
* 如果发现了log冲突(相同的log索引但term不一致): 删除此冲突log并用收到的所有后续log覆盖
* 追加收到的新log
* 如果`leaderCommit > commitIndex`: 设置`commitIndex = min(leaderCommit, 收到的最新log索引)`
* 重置选举超时(在固定时间基础上略微随机调节)

## 流程

###### 所有节点通用的运行流程
* 如果`commitIndex > lastApplied`: 在状态机上执行`log[++lastApplied]`
* 如果发现接收的RPC请求和回复中的`term > currentTerm`: 设置`currentTerm = term`并把自己转为follower

###### 所有follower节点的运行流程
* 处理和回复candidate发来的`RequestVote`和leader发来的`AppendEntries`
* 如果选举超时: 把自己转为candidate

###### 所有candidate节点的运行流程
* 刚转成candidate时开始选举,执行:
  * `currentTerm++`
  * 重置选举超时(在固定时间基础上略微随机调节)
  * 发送`RequestVote`给其它节点
* 如果收到半数以上节点`RequestVote`的回复是投票成功: 自己转成leader
* 如果从新的leader收到`AppendEntries`: 把自己转为follower
* 如果选举超时: 按刚转成candidate时一样重新选举

###### leader节点的运行流程
* 刚转成leader时: 广播心跳(空`entries`的`AppendEntries`)给其它节点, 后续还需在某节点可能触发选举超时前广播心跳
* 如果从客户端收到命令: 追加命令log到log[], 然后直到命令被leader执行才回复客户端
* 如果`最新的log索引 >= nextIndex[]中的某个follower`: 给它发送从其`nextIndex`开始所有log的`AppendEntries`
  * 如果回复成功: 在`nextIndex[]`和`matchIndex[]`中更新它的`nextIndex`和`matchIndex`
  * 如果回复失败: 如果由于log一致性原因导致, 则自减nextIndex[]中它的nextIndex并重试`AppendEntries`
* 如果存在`N`满足`N > commitIndex`,且`超半数matchIndex[]都 >= N`,且`log[N].term == currentTerm`: 设置`commitIndex = N`
