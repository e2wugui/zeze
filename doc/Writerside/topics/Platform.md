# Platform

Zeze支持多个开发平台。platform是project属性，需要明确指定。

## java
服务器开发平台。

## cs
服务器或者客户端。

## ts
纯TypeScript环境。一般是ts客户端，不包括完整环境。

## cs+ts
客户端使用Unity+TypeScript(依赖puerts)
1.	把 zeze/Zeze 发布到你的项目，直接拷贝代码或者需要自己编译发布二进制。
2.	把 zeze/TypeScript/ts/ 下的 zeze.ts 拷贝到你的 typescript 源码目录。
3.	依赖：npm install https://github.com/inexorabletash/text-encoding.git
4.	把 zeze/Zeze/Services/ToTypeScriptService.cs 文件中 #if USE_PUERTS 宏内的代码拷贝到
      你的c#源码目录下的ToTypeScriptService.cs 文件中。当然这里可以另起一个文件名。
5.	把 typeof(ToTypeScriptService) 加到 puerts 的 Bindings 列表中。
6.	然后使用 puerts 的 unity 插件菜单生成代码。
7.	定义 solutions.xml 时，ts客户端要处理的协议的 handle 设置为 clientscript.
8.	使用 gen 生成协议和框架代码。
9.	例子可以看看 https://gitee.com/e2wugui/zeze-unity.git
10.	不知道怎么发布依赖，现在测试运行是把encoding.js encoding-indexes.js 拷贝到output
       下。其中 encoding.js 改名为 text-encoding.js。

## cxx+ts
客户端使用Unreal(cxx)+TypeScript(依赖puerts)
1.	把zeze\cxx下的所有代码拷贝到你的源码目录并且加到项目中。除了Lua相关的几个文件。
2.	把 zeze/TypeScript/ts/ 下的 zeze.ts 拷贝到你的 typescript 源码目录。
3.	依赖 npm install https://github.com/inexorabletash/text-encoding.git
4.	安装puerts，并且生成ue.d.ts。
5.	定义 solutions.xml 时，ts客户端要处理的协议的 handle 设置为 clientscript.
6.	使用 gen 生成协议和框架代码。
7.	zeze\cxx\ToTypeScriptService.h 里面的宏 ZEZEUNREAL_API 改成你的项目的宏名字。
8.	例子 https://gitee.com/e2wugui/ZezeUnreal.git
9.	不知道怎么把依赖库(text-encoding)发布到unreal中给puerts用，可以考虑把encoding.js
      encoding-indexes.js
10.	拷贝到Content\JavaScript\下面，其中 encoding.js 改名为 text-encoding.js。

## lua
纯lua环境，不包括完整框架。

## cs+lua
客户端使用Unity(csharp)+lua
1.	需要选择你的Lua-Bind的类库，实现一个ILua实现
      （参考 Zeze.Service.ToLuaService.cs）。
2.	定义 solutions.xml 时，客户端要处理的协议的 handle 设置为 clientscript.
3.	使用例子：zeze\UnitTestClient\Program.cs。

## luaclient
纯lua环境。其中bean在lua中使用变量名字作为table的key。

## cs+luaclient
cs+lua特别版。参考cs+lua。

## cxx+lua
客户端使用Unreal(cxx)+lua
1.	依赖lualib, 需要设置includepath
2.	直接把cxx下的所有代码加到项目中。除了ToTypeScript相关的。
3.	定义 solutions.xml 时，客户端要处理的协议的 handle 设置为 clientscript.
4.	使用例子：zeze\UnitTestClientCxx\main.cpp

## conf+cs
cs系列化的可独立发布的版本，不依赖Zeze库。目前用于unity配置。这个版本的bean生
成代码很简洁。

## conf+cs+net
cs系列化的可独立发布的版本，不依赖Zeze库，带了网络和日志增量同步。这个版本的bean
生成代码很简洁。导出源码的批处理程序参考zeze/Zege/confcsnet.bat。一般项目准备步骤
如下：
1.	定义自己solution.xml，并生成。
2.	把confcsnet.bat拷贝到将要独立发布的项目目录下并执行。
3.	在将要独立发布的项目配置中加上宏“USE_CONFCS”。
