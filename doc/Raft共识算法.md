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
* `term`: 自己的currentTerm
* `candidateId`: 自己的节点ID
* `lastLogIndex`: 自己的log[]中的最新log的索引
* `lastLogTerm`: 自己的log[]中的最新log的term
###### 回复字段
* `term`: 处理者的`currentTerm`, 用于给发起者更新
* `voteGranted`: 是否给发起者投票成功
###### 处理请求流程
* 如果`term < currentTerm`: 投票失败
* 如果`votedFor`为null或等于请求的`candidateId`, 且请求的`lastLogIndex`和`lastLogTerm`都不小于自身的对应值:
  * 更新`votedFor`, 重置选举超时(在固定时间基础上略微随机调节), 并回复投票成功
  * 否则投票失败
###### 处理回复流程
* 如果达到半数以上节点`RequestVote`的回复是投票成功: 自己转成leader


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
* 如果`commitIndex > lastApplied`: 在状态机上执行`log[++lastApplied]`中的命令
* 如果发现接收的RPC请求和回复中的`term > currentTerm`: 设置`currentTerm = term`并把自己转成follower

###### 所有follower节点的运行流程
* 处理和回复candidate发来的`RequestVote`和leader发来的`AppendEntries`
* 如果选举超时: 把自己转成candidate

###### 所有candidate节点的运行流程
* 刚转成candidate时开始选举,执行:
  * `currentTerm++`
  * 设置`votedFor=自己的节点ID`
  * 重置选举超时(在固定时间基础上略微随机调节)
  * 广播`RequestVote`给其它节点
* 如果从新的leader收到`AppendEntries`: 把自己转成follower
* 如果选举超时: 按刚转成candidate时一样重新选举

###### leader节点的运行流程
* 刚转成leader时: 广播心跳(空`entries`的`AppendEntries`)给其它节点, 后续还需在某节点可能触发选举超时前广播心跳
* 如果从客户端收到命令: 追加命令log到log[], 然后直到命令被leader执行才回复客户端
* 如果`最新的log索引 >= nextIndex[]中的某个follower`: 给它发送从其`nextIndex`开始所有log的`AppendEntries`
  * 如果回复成功: 在`nextIndex[]`和`matchIndex[]`中更新它的`nextIndex`和`matchIndex`
  * 如果回复失败: 如果由于log一致性原因导致, 则自减nextIndex[]中它的nextIndex并重试`AppendEntries`
* 如果存在`N`满足`N > commitIndex`,且`超半数matchIndex[]都 >= N`,且`log[N].term == currentTerm`: 设置`commitIndex = N`

## 重新整理的伪实现(持续完善中)

#### 状态
* `int selfId` [readonly] 自身节点ID. 有效值>=0
* `int leaderId` 当前的leader节点ID. -1表示自己是follower, -2表示自己是candidate, 初始为-1
* `int votedFor` [持久化] 为currentTerm投票的节点ID. 初始为-1表示没有
* `int successCount` [持久化] 给自己投票成功的次数/复制log请求成功次数
* `long timeout` 下次投票的超时时间戳(毫秒). `<=当前时间戳`表示应该触发超时, 初始为`当前时间戳+INIT_TIMEOUT+random(150)`
* `long currentTerm` [持久化] 已知的最新term. 初始为0, 实际有效值一定>0
* `list<LogEntry> logEntries` [持久化] 已存储的log序列. 按log的索引需从小到大连续保存, 初始为空
  * `long LogEntry.index` 每条log的索引. 在log序列中不会重复
  * `long LogEntry.term` 每条log的term值
  * `Command LogEntry.cmd` 每条log的命令对象. 有执行和序列化接口
* `long commitIndex` 当前最大的已经提交的log索引. 初始为0
* `long lastApplied` 当前最大的已经执行的log索引. 初始为0
* `long[] nextIndexes` [仅leader用] 各节点最大已被复制的log索引. 初始为0
* `long[] appendingTimeouts` [仅leader用] 各节点的AppendEntries请求超时时间戳(毫秒). 初始为0表示无超时

#### 流程(无阻塞的事件响应模式)
* if selfId != leaderId:
  * while 收到通信协议:
    * if 收到RequestVote请求:
      * if currentTerm > RequestVote.term:
        * 回复投票失败, 重新执行当前流程
      * if currentTerm < RequestVote.term:
        * currentTerm = RequestVote.term
      * if (votedFor == -1 || votedFor == RequestVote.candidateId) && RequestVote.lastLogIndex >= logEntries[newest].index && RequestVote.term >= logEntries[newest].lastLogTerm
        * votedFor = RequestVote.candidateId
        * 回复投票成功
      * else
        * 回复投票失败
    * else if 收到AppendEntries请求:
      * if currentTerm > AppendEntries.term:
        * 回复失败或不回复, 重新执行当前流程
      * leaderId = AppendEntries.leaderId
      * votedFor = -1
      * timeout = 当前时间戳 + LEADER_TIMEOUT + random(150)
      * if currentTerm < RequestVote.term:
        * currentTerm = RequestVote.term
      * if AppendEntries.prevLogIndex != 0 && AppendEntries.prevLogTerm != 0:
        * 如果logEntries里找不到AppendEntries.prevLogIndex和AppendEntries.prevLogTerm对应的log:
          * 回复失败, 重新执行当前流程
      * if AppendEntries.entries.size > 0
        * 取entry = AppendEntries.entries的首个
        * 如果在logEntries中找到entry.index
          * 删除logEntries[>=entry.index]的项
        * 追加AppendEntries.entries到logEntries
        * if commitIndex < AppendEntries.leaderCommit:
          * commitIndex = min(AppendEntries.leaderCommit, logEntries最大的index)
          * while lastApplied < commitIndex:
            * 执行logEntries[++lastApplied].cmd
    * else if 收到RequestVote回复:
      * if leaderId != CANDIDATE(-2):
        * 丢弃(只应该由candidate处理)
      * if currentTerm != RequestVote.term
        * 丢弃
      * if ++successCount > NODE_COUNT / 2
        * leaderId = selfId
        * votedFor = -1
        * successCount = 0
        * nextIndexes和appendingTimeouts全部清0
    * else if 收到AppendEntries回复:
      * 丢弃(只应该由leader处理)
    * else if 收到客户端请求:
      * if leaderId >= 0:
        * 回复leaderId让客户端重新发请求给leader
      * else
        * 回复失败
  * else if timeout超时:
    * currentTerm++
    * leaderId = CANDIDATE(-2)
    * votedFor = selfId
    * timeout = 当前时间戳 + VOTE_TIMEOUT + random(150)
    * successCount = 1
    * 广播RequestVote(term=currentTerm, candidateId=selfId, lastLogIndex=logEntries[newest].index, lastLogTerm=logEntries[newest].term)
* else
  * while 收到通信协议:
    * if 收到RequestVote请求:
      * if currentTerm < RequestVote.term:
        * leaderId = FOLLOWER(-1)
        * timeout = 当前时间戳 + LEADER_TIMEOUT + random(150)
        * 保留RequestVote请求下次处理
      * else
        * 回复失败
    * else if 收到AppendEntries请求:
      * if currentTerm < AppendEntries.term:
        * leaderId = AppendEntries.leaderId
        * timeout = 当前时间戳 + LEADER_TIMEOUT + random(150)
        * 保留AppendEntries请求下次处理
      * else
        * 回复失败
    * else if 收到RequestVote回复:
      * 丢弃(只应该由candidate处理)
    * else if 收到AppendEntries回复:
      * 如果回复成功:
        * 取nodeId=AppendEntries.nodeId
        * nextIndexes[nodeId] = AppendEntries.entries[最新的index] + 1
        * 计算出当前最大的commitIndex, 满足commitIndex <= nextIndexes中超半数的值
        * while lastApplied < commitIndex:
          * 执行logEntries[++lastApplied].cmd
        * 如果当前客户端请求中的命令都被leader执行了, 则回复成功
        * 取nextIndex=nextIndexes[nodeId]
        * if nextIndex <= logEntries的最大index:
          * 继续给nodeId发送AppendEntries(term=currentTerm, leaderId=selfId, prevLogIndex=logEntries[nextIndex-1].index or 0, lastLogTerm=logEntries[nextIndex-1].term or 0, entries=logEntries[nextIndex...], leaderCommit=commitIndex)
          * appendingTimeouts[nodeId] = 当前时间戳 + APPEND_TIMEOUT (结果为0则置1)
        * else
          * appendingTimeouts[nodeId] = 0
      * else if currentTerm < AppendEntries.term:
        * leaderId = FOLLOWER(-1)
        * timeout = 当前时间戳 + LEADER_TIMEOUT + random(150)
      * else
        * nextIndexes[nodeId]--
    * else if 收到客户端请求:
      * if 请求的命令列表为空:
        * 直接回复成功, 重新执行当前流程
      * else
        * 为每个命令定义index(从logEntries的最大index开始自增,初始为1)形成log并追加到自己的logEntries中
        * 遍历appendingTimeouts, 如果appendingTimeouts[nodeId] == 0:
          * 取nextIndex=nextIndexes[nodeId]
          * 给nodeId发送AppendEntries(term=currentTerm, leaderId=selfId, prevLogIndex=logEntries[nextIndex-1].index or 0, lastLogTerm=logEntries[nextIndex-1].term or 0, entries=logEntries[nextIndex...], leaderCommit=commitIndex)
          * appendingTimeouts[nodeId] = 当前时间戳 + APPEND_TIMEOUT (结果为0则置1)
  * while appendingTimeouts中有appendingTimeouts[nodeId]超时:
    * appendingTimeouts[nodeId] = 当前时间戳 + APPEND_TIMEOUT (结果为0则置1)
    * 给nodeId发送空的AppendEntries
