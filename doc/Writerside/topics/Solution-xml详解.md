# Solution.xml

## Solution
解决方案。一个应用系统可以包含多个解决方案。配置文件名字可以不叫solution.xml。但
存在多个解决方案配置文件时，建议按solution.xxx.xml方式命名文件。
## Import
引入其他solution配置文件，这样就可以使用其他文件内定义的Bean。两个soltion配置文
件可以循环Import。如：
```
<import file="solution.linkd.xml"/>
```
## Module
模块在Zeze里是定义bean，table，protocol，rpc等的地方。这个名字和系统功能划分的“模
块“的概念一致。Zeze本身没有为模块提供什么重要功能，在生成代码时，主要提供内部对
象的名字空间。module可以包含module。module.id必须唯一，必须在solution.
ModuleIdAllowRanges规定的范围内。当系统包含多个solution时，范围不能重叠。最终保
证了module.id在整个系统内唯一。生成代码时，除了生成模块内定义的对象，还会为每个
模块生成一个入口文件。需要处理的协议会在这个入口文件内生成空的处理函数。
## Bean
Bean是Zeze的核心对象，用来定义数据结构。其中的variable描述变量名字和类型。variable
可以自由增删变量（即使系统上线以后），自动兼容旧的数据结构。variable.id在bean内唯
一，不能复用（variable删除以后，新增的变量再次使用相同的id被认为是一个反悔操作，
此时variable.type必须和删除前的兼容）。variable.type可以是另一个bean。Bean的命名建
议以“B“开头。这样需要创建Bean时，输入B即可得到编辑器的提示。
## Table
定义Key-Value存储表。
```
<table name=”tTrade“ key=”long” value=”BTradesession” />
```
## Protocol
定义协议。
```
<protocol name=”Trade” argument=”BTrade” handle=”server”/>
```
argument是一个Bean。
handle 表示协议在哪里被处理。
## Rpc
定义Rpc。
```
<rpc name=”Trade” argument=”BTradeArgument” result=”BTradeResult” 
handle=”server”/>
```
argument是一个Bean。result是一个Bean。handle 表示协议在哪里被处理。
## Project
定义项目，对应一个进程。一个solution一般拥有两个项目。一个server，一个client。
可能还有一个test。
```
<project name="GameServer" scriptdir="src" platform="java">
<project name="GameServer" platform="cs">
```
## Service
网络服务定义。协议在网络服务里面注册。网络服务管理连接以及提供网络事件和收到的协
议的派发。
```
<service name="Server" handle="server”>
<module ref="Role"/>
</service>
```
* module ref 引用模块。被一个Service引用的模块内定义的协议会被自动注册。
* handle 引用模块内定义的协议的符合这个类别的，在这个服务里面注册和派发。
* Protocol.Handle &amp; Rpc.Handle &amp; Service.Handle
处理标签包含：server,client, serverscript, clientscript。采用server,client的叫法，仅仅因为
这样比较符合网络程序功能通常的划分。支持多个标签，用”,”隔开。

## Type

Bean变量支持的类型以及在不同语言内的实际类型。

| type    | Java                                           | C#                                   | Lua            | TypeScript  |
|---------|------------------------------------------------|--------------------------------------|----------------|-------------|
| bool    | boolean                                        | bool                                 | boolean        | boolean     |
| byte    | byte                                           | byte                                 | number(int64)  | number      |
| short   | short                                          | short                                | number(int64)  | number      |
| int     | int                                            | int                                  | number(int64)  | number      |
| long    | long                                           | long                                 | number(int64)  | bigint      |
| float   | float                                          | float                                | number(double) | number      |
| double  | double                                         | double                               | number(double) | number      |
| binary  | Zeze.Net.Binary                                | Zeze.Net.Binary                      | string         | Uint8Array  |
| string  | String                                         | string                               | string         | string      |
| map     | CollMap2&lt;Bean&gt;，CollMap1&lt;Integer&gt;   | PMap2&lt;Bean&gt;，PMap1&lt;int&gt;   | table          | Map         |
| list    | CollList2&lt;Bean&gt;，CollList1&lt;Integer&gt; | PList2&lt;Bean&gt;，PList1&lt;int&gt; | table          | Array       |
| set     | CollSet1&lt;Integer&gt;                        | PSet1&lt;int&gt;                     | table          | Set         |
| dynamic | DynamicBean                                    | DynamicBean                          | table          | DynamicBean |













