# 第三章 Quick Start

进程本身直接提供网络服务，这里的例子快速展现zeze的核心能力。这里没有考虑完整的
网络框架支持，稍后讲到的Arch是zeze的网络框架，包含网关和主逻辑服务的连接以及架
构。Zeze最核心的能力是事务的支持，只需要三个步骤就能开始使用zeze事务。

* 定义数据结构
* 生成代码
* 访问Table以及使用自定义数据结构访问修改数据

下面稍微详细的用例子介绍三个步骤。

* 定义数据结构
```
<?xml version="1.0" encoding="utf-8"?>
<solution name="QuickStart" ModuleIdAllowRanges="1-100,101">
<module name="Role" id="1">
<bean name="BRole">
<variable id="1" name="Level" type="int"/>
<variable id="2" name="Experience" type="long"/>
</bean>
<bean name=”BBag“>
<variable id="1" name="Items" type=”list[int]”/>
</bean>
<table name=”tRole” key=”long” value=”BRole”/> key is roleid
<table name=”tBag” key =”long” value=”BBag”/> key is roleid
<bean name=”BAddExperience”>
<variable id="1" name="Experience" type="long"/>
</bean>
客户端直接增加经验是不合理的，但这个例子就是这样做了，不管作弊啦。
<rpc name=”AddExperience” argument=”BAddExperience”
　　TransactionLevel=“Serializable“ handle=”server”/>
</module>
<project name="GameServer" scriptdir="src" platform="java">
<service name="Server" handle="server”>
<module ref="Role"/>
</service>
</project>
</solution>
```
* 生成代码

把上面的xml保存为solution.xml，切换到文件所在目录，执行Gen.exe solution.xml。在当
前目录下会创建GameServer目录(project name)，在里面会找到生成的代码。这里先不细说
了。

* 使用例子 (java)

```
// Gen.exe 会根据定义自动生成空的处理函数。里面的代码就是自己的实现了。
@Override
long ProcessAddExperience(QuickStart.Role.AddExperience r) {
	var session = ProviderUserSession.get(r); // 这是个魔法，反正拿到会话了。
　　
　　var roleId = session.getRoleId();
　　long newExperience = r.Argument.getExperience();
　　
	var role = _tRole.getOrAdd(roleId);
　　role. setExperience(role.get Experience() + newExperience);
　　while (role.getExperience() >= ExperienceConfig.get(role.getLevel())) {
　　    role.setExperience(role.get Experience() - ExperienceConfig.get(role.getLevel()));
　　	role.setLevel(role.getLevel() + 1);
　　	if (role.getLevel() % 10 == 0) {
　　		addItemToBag(roleId, LevelRewardItemConfig.get(role.getLevel()));
　　}
　　}
　　return 0;
}

// 多数时候，Bag是另一个模块(module)。这里都定义到一起了。
void addItemToBag(long roleid, int itemId) {
	var bag = _tBag.getOrAdd(roleId);
	if (bag.getItems().size() > 100)
		throw new RuntimeException(“Bag Is Full”); // 别担心，包满了，所有修改都会回滚。
	bag.getItems().add(itemId);
}
```

## 上手准备

1. git clone https://gitee.com/e2wugui/zeze
2. 运行 zeze\gen_use_publish.bat。
3. 再次 Build All，应该就正常了。
4. Csharp版使用vs2022或其他兼容编辑器打开zeze/Zeze.sln
5. Java版使用idea打开 zeze/ZezeJava即可正常Build All。

## Java开发准备 (如果只关注C#开发,可跳过)

1. JDK 21 任意发行版均可
   - 推荐: https://adoptium.net/zh-CN/temurin/archive/?version=21
   - 备选: https://jdk.java.net/21/
2. IntelliJ IDEA 免费社区版(Community)即可, 2023.3版以上
   - https://www.jetbrains.com/idea/download/
3. (可选) Maven: https://maven.apache.org/download.cgi

## C#开发准备 (如果只关注Java开发,可只安装.NET SDK)

1. .NET 8 SDK (如果安装下面的Visual Studio,可以不用单独安装这个)
   - https://dotnet.microsoft.com/en-us/download/dotnet/8.0 (通常选择Windows, x64)
2. Visual Studio 2022 (可用免费社区版,但需要联网激活,且只能个人或小规模商用)
   - Windows版本: https://visualstudio.microsoft.com/zh-hans/downloads/ (必选组件: .NET桌面开发; 可选组件: Node.js开发)
3. (可选) VSCode (完全免费的轻量级IDE)
   - https://code.visualstudio.com/ (安装官方C#插件)

## Java编译

1. 启动IDEA, 打开zeze框架中的 ZezeJava 目录, 会自动加载4个模块(框架核心+测试+2个示例)
2. 如果编译找不到某些类, 需要执行下面的"生成代码"

## C#编译

1. 仅用SDK: 在zeze框架根目录下使用命令行编译: dotnet build Zeze.sln
2. 使用VS2022: 启动VS2022, 打开zeze框架根目录下的Zeze.sln, 执行编译命令
3. 如果编译找不到某些类, 需要执行下面的"生成代码"

## Java Sample

1. 需要先执行上面的C#编译Gen工程
2. 在 ZezeJava/ZezexJava, ZezeJava/ZezeJavaTest 目录下执行 gen.bat

## C# Sample

1. 需要先执行上面的C#编译Gen工程
2. 在 Sample, UnitTest 目录下执行 gen.bat

## 分布式架构

![arch](../images/arch.png)

上图是Zeze默认的框架结构，具有一定通用性。某些情况下，自己可以搭建全新的架构。

* Linkd 连接进程，负责负载分配。Zeze提供一个默认实现，配置以后，生成代码，即可零
开发直接使用。生成代码的目的是为了支持重载某些方法，实现定制。一般来说Arch提供
的linkd功能足够了，实际开发量很低。除非你需要对linkd规则做很大的定制。
* GameServer 主服务器，实现业务逻辑。开发的主要产出。
* ServiceManager 服务注册和发现服务器。Zeze提供，不需要开发。
* GlobalCacheManager 一致性缓存支持服务器。Zeze提供，不需要开发。
* Database 后端数据库，支持Mysql,Sqlserver,Tikv。将来Zeze会根据需要支持更多的数据库。

## 单实例架构

系统内只有一个GameServer，客户端直接连接GameServer。此时不需要ServiceManager
和GlobalCacheManager。由于分布式架构本质上也包含了单实例架构，所以这个模式的例
子被删除了（可用从git的历史中找到）。需要说明的是，不管那种架构，业务开发代码几
乎一样，架构应该仅影响程序框架。如果需要实现全新的架构，最好不要影响业务开发，方
便需要的时候进行迁移。

## 创建Zeze应用（Java）

跟创建普通Idea项目一样创建工程，把项目到Zeze依赖建立好，一个普通的Zeze应用就
创建好了。这里实际上不需要什么特殊操作，因为Zeze虽然是一个框架，但没有太多侵入
要求，本身是作为一个库提供的。当然，为了发挥Zeze的能力，solution.xml是必要的。下
面是第一个简单solution.xml。

```
<?xml version="1.0" encoding="utf-8"?>

<solution name="Game" ModuleIdAllowRanges="1-1000">
　　Game是解决方案的名字，它将作为生成代码的根名字空间。
　　<import file="../ZezeJava/solution.zeze.xml"/> 路径需要修改为正确的相对目录。
	<module name="Login" id="1" hot="true"> 自定义模块
		<bean name="BCreateRole"> 自定义Bean
			<variable id="1" name="Name" type="string"/>
		</bean>
		<bean name="BRole">
			<variable id="1" name="Id" type="long"/>
			<variable id="2" name="Name" type="string"/>
		</bean>
		<bean name="BRoles">
			<variable id="1" name="RoleList" type="list" value="BRole"/>
			<variable id="2" name="LastLoginRoleId" type="long"/>
		</bean>
		<rpc name="CreateRole" argument="BCreateRole"
　　result="BRole" handle="server"/> 自定义Rpc，客户端发送给服务器请求数据。
		<rpc name="GetRoleList" result="BRoles" handle="server"/>
		<bean name="BRoleId">
			<variable id="1" name="Id" type="long"/>
		</bean>
		<bean name="BAccount">
			<variable id="1" name="Roles" type="set[long]"/>
			<variable id="2" name="LastLoginRoleId" type="long"/>
		</bean>
		<table name="taccount" key="string" value="BAccount"/>
		<table name="trole" key="long" value="BRole"/>存储Role的数据表
		<table name="trolename" key="string" value="BRoleId"/>
	</module>
	<project name="server" gendir="."
　　scriptdir="src" platform="java" GenTables="">
　　name=”server” 项目名字，必须和idea创建的功能名字一样。以后生成代码会放
到这个工程目录下面。
　　gendir="." 生成代码输出的根目录，solution.xml放在server的上一级目录时，就
是当前目录。
　　scriptdir="src" 源代码目录相对路径。默认是src，一般idea需要改成
“src/main/java/”
		<service name="Server" handle="server" base="Zeze.Arch.ProviderService">
			<module ref="Login"/>这个网络服务包含的模块
		</service>定义网络服务
		<ModuleStartOrder>
		</ModuleStartOrder>
		<service name="ServerDirect" handle="server,client"
base="Zeze.Arch.ProviderDirectService">
		</service>分布式情况下，Server之间互联的网络服务定义。
	</project>
```

代码生成以后，工程server/Gen包含生成代码，这里的每次生成都会被覆盖，这些代码主
要是Bean，Table，Protocok，Rpc等。MyApp/src里面也会生成一些代码，这些代码是协
议处理框架的实现部分，不会全部被覆盖。其中最主要的是（对应上面的solution.xml）
MyApp.Login.ModuleLogin.java。一般如下样子：

```
package MyApp.Login;

import Game.App;
import Zeze.Arch.ProviderUserSession;
import Zeze.Component.AutoKey;
import Zeze.Hot.HotService;
import Zeze.Transaction.Procedure;

public final class ModuleLogin extends AbstractModule {
	public void Start(App app) {
	}
	public void Stop(App app) {
	}
	@Override
	protected long ProcessCreateRoleRequest(CreateRole rpc) {
		// 在这里编写实现逻辑，实现例子参见
　　// zeze/ZezeJava/ZezexJava/server/src/Game/Login/ModuleLogin.java
		return Procedure.NotImplement;
	}
	@Override
	protected long ProcessGetRoleListRequest(GetRoleList rpc) {
		// 在这里编写实现逻辑，实现例子参见
　　// zeze/ZezeJava/ZezexJava/server/src/Game/Login/ModuleLogin.java
		return Procedure.NotImplement;
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
	public ModuleLogin(Game.App app) {
		super(app);
	}
	// ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
```

## 创建Arch.Linkd（Java）

* Solution.linkd.xml
```
<?xml version="1.0" encoding="utf-8"?>

<solution name="Zezex" ModuleIdAllowRanges="10000-10999">
	Zezex是解决方案的名字，用于生成代码的根名字空间。整个解决方案使用一个名字是
可以的，参考上节的solution.xml的例子，这里也可以使用Game。为了避免给阅读ZezexJava
例子工程造成困扰，这里保留了Zezex的名字。
	<module name="Linkd" id="10000"> linkd专有的模块
		<bean name="BAuth">
			<variable id="1" name="Account" type="string"/>
			<variable id="2" name="Token" type="string"/> security. maybe password
		</bean>

		<rpc name="Auth" argument="BAuth" handle="server">
			<enum name="Success" value="0"/>
			<enum name="Error"   value="1"/>
		</rpc>
	</module>

	<project name="linkd" gendir="." scriptdir="src" platform="java">
		<service name="LinkdService" handle="server" base="Zeze.Arch.LinkdService">
			<module ref="Linkd"/>
		</service>

		<service name="ProviderService" handle="client"
base="Zeze.Arch.LinkdProviderService">
		</service>
	</project>

</solution>
```
* App.java::Start

Solution.linkd.xml生成代码时，会生成linkd/src/Zezex/App.java，其中Start方法是程序初
始化启动的函数。默认生成的Start是没有引入Zeze.Arch模块的，使用它需要自己加入几
行代码。下面是来自Zezex例子，变动部分看斜体部分。

```
	public void Start(int serverId, int linkPort, int providerPort) throws Exception {
		//1. 默认生成的Start是没有参数的，由于Start是用户代码，
　　//Zezex已经把它修改成支持更多参数。
　　
　　// 2. 下面Config相关的设置已经被定制了。
		var config = Config.load("linkd.xml");
		if (serverId != -1)
			config.setServerId(serverId);
		if (linkPort != -1) {
			config.getServiceConfMap().get("LinkdService").forEachAcceptor((a) ->
a.setPort(linkPort));
		}
		if (linkPort != -1) {
			config.getServiceConfMap().get("ProviderService").forEachAcceptor((a) ->
a.setPort(providerPort));
		}
		createZeze(config);
		createService();

		// 3. 这几行代码不是生成的，而是使用Zeze.Arch的初始化代码。
		LinkdProvider = new LinkdProvider();
		LinkdApp = new LinkdApp("Game.Linkd", Zeze, LinkdProvider, ProviderService,
LinkdService, LoadConfig());
		createModules();
		// Start
		Zeze.start(); // 启动数据库
		startModules(); // 启动模块，装载配置什么的。

		//4. 这样代码不是生成的，是应用特殊的初始化。
	AsyncSocket.setSessionIdGenFunc(PersistentAtomicLong.getOrAdd(LinkdApp.getName
())::next);
		startService(); // 启动网络. after setSessionIdGenFunc
		//5. 这是Zeze.Arch的初始化代码。
		LinkdApp.registerService(null);
	}
```

先说声抱歉，由于没有花时间去整理第一次空项目生成代码的样子，上面的例子直接来自
ZezexJava。如果你发现你的第一次生成的代码，和斜体部分除外还对应不上，请自行判断
一下。总的来说，第一次生成的代码就是一个完整的Zeze应用，但是不包括Zeze.Arch，
上面斜体部分的代码主要是使用Zeze.Arch需要做的改动。

<b>创建Arch.Serverd（Java）</b>

Solution.xml 参见上上节的创建Zeze应用。这里只说明一下App.java::Start怎么引入
Zeze.Arch。

```
	public void Start(int serverId, int providerDirectPort) throws Exception {
　　//1. 默认生成的Start是没有参数的，由于Start是用户代码，
　　//Zezex已经把它修改成支持更多参数。
　　
　　// 2. 下面Config相关的设置已经被定制了。
		var config = Config.load("server.xml");
		if (serverId != -1) {
			config.setServerId(serverId); // replace from args
		}
		var commitService = config.getServiceConf("Zeze.Dbh2.Commit");
		if (null != commitService) {
			commitService.forEachAcceptor((a) -> {
				a.setPort(a.getPort() + config.getServerId());
			});
		}
		if (providerDirectPort != -1) {
			final int port = providerDirectPort;
			config.getServiceConfMap().get("ServerDirect").forEachAcceptor((a) ->
a.setPort(port));
		}
		// create
		createZeze(config);
		createService();

		// 3. 引入Zeze.Arch的代码
		Provider = new ProviderWithOnline();
		Provider.setControlKick(BKick.eControlReportClient);

		ProviderDirect = new ProviderDirectWithTransmit();
		ProviderApp = new ProviderApp(Zeze, Provider, Server,
				"Game.Server.Module#",
				ProviderDirect, ServerDirect, "Game.Linkd", LoadConfig());
		Provider.create(this);

		createModules();
		// 4. 程序运行过程中也会动态生成代码并编译，这些代码一般看不到，这里把代
码生成出来，用来调试的。
		if (GenModule.instance.genFileSrcRoot != null) {
			System.out.println("---------------");
			System.out.println("New Source File Has Generate. Re-Compile Need.");
			System.exit(0);
		}

		// 5. Zeze组件的引入，组件的功能参见后面Component,Collections相关部分文档。
		taskModule = new TaskBase.Module(getZeze());
		LinkedMapModule = new LinkedMap.Module(Zeze);
		DepartmentTreeModule = new DepartmentTree.Module(Zeze, LinkedMapModule);

		Zeze.getTimer().initializeOnlineTimer(ProviderApp);

		// start
		Zeze.start(); // 启动数据库
		startModules(); // 启动模块，装载配置什么的。

		// 6. Zeze.Arch 初始化
		Provider.start();
		PersistentAtomicLong socketSessionIdGen =
PersistentAtomicLong.getOrAdd("Game.Server." + config.getServerId());
		AsyncSocket.setSessionIdGenFunc(socketSessionIdGen::next);
		startService(); // 启动网络
		// 7. Zeze.Arch 启动
		// 服务准备好以后才注册和订阅。
		ProviderApp.startLast(ProviderModuleBinds.load(), modules);
	}

```
