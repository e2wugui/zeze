# provider.module.binds.xml

配置模块在provider上的分布和运行。先给一个整体例子，后面详细说明。
```
<?xml version="1.0" encoding="utf-8"?>
<ProviderModuleBinds>
	<!-- 动态模块 -->
	<module name="Zeze.World" ConfigType="Dynamic"/>
	<!-- 特别指定运行provider的模块 -->
	<module name="Zeze.Special" ConfigType="Special" providers="0,1,2"/>
	<!-- 本来是默认模块，但为了在provider=0上面运行，需要明确指名所有provider="*"，参考后面的ProviderNoDefaultModule -->
	<module name="Zeze.RedirectToServerSample" ConfigType="Default" providers="*"/>
	<!-- 用来给模块提供默认配置 -->
	<defaultModule ChoiceType=“ChoiceTypeDefault” ConfigType=“Default”>
	<!-- 指定的provider上面不再运行默认模块 -->
	<ProviderNoDefaultModule providers="0,1,2"/>
</ProviderModuleBinds>
```

## module(name, ConfigType, ChoiceType, providers)
* name
	必须是全名。

* ConfigType：
	Special（特别的指定了运行provider的模块，这个类型实际上不建议配置，历史原因保留着），
	Dynamic（动态模块，由程序动态绑定选择运行的provider），
	Default（没有提供module配置的就是默认模块）

* ChoiceType link选择provider的算法选择。
	ChoiceTypeHashAccount 按账号的hash选择provider。 
	ChoiceTypeHashRoleId 按角色Id的hash选择provider。
	ChoiceTypeHashSourceAddress 根据源ip地址的hash选择provider。
	ChoiceTypeFeedFullOneByOne 按顺序选择provider，每个provider尽量先达到配置的在线量才选择下一个，用来把玩家往一起赶。
	ChoiceTypeDefault 按负载均衡算法选择provider。

* providers
	配置模块运行的provider。默认模块在所有的provider上面运行。选择指定的provider加上ProviderNoDefaultModule可以把provider
	配置成专用服务器。

## defaultModule(ChoiceType, ConfigType)
* 为所有的module提供默认配置。

## ProviderNoDefaultModule(providers)
* 表示指定的providers上面不运行默认模块。

## 专用服务器例子
```
<?xml version="1.0" encoding="utf-8"?>
<ProviderModuleBinds>
	<!-- 本来是默认模块，但为了在provider=0上面运行，需要明确指名所有provider="*"，参考后面的ProviderNoDefaultModule -->
	<module name="Zeze.RedirectToServerSample" ConfigType="Default" providers="*"/>
	<!-- 指定的provider上面不再运行默认模块 -->
	<ProviderNoDefaultModule providers="0"/>
</ProviderModuleBinds>
```
上面的配置表明provider=0不运行其他的默认模块，但是Zeze.Special2会运行，他是明确配置的，优先级高。
这样Zeze.Special2就可以在0上面专门提供服务，同时又可以在其他provider上面运行。实际用处就是有个RedirectToServerSample
模块，通过@RedirectToServer在provider间互发协议，它的主服务器是provider=0，同时其他provider作为客户端。
这样配置就能达到provider=0只运行RedirectToServerSample的主服务，没有其他负载过来。

## 专用服务器例子2
```
<?xml version="1.0" encoding="utf-8"?>
<ProviderModuleBinds>
	<!-- 特别指定运行provider的模块 -->
	<module name="Zeze.Special" ConfigType="Special" providers="0,1,2"/>
	<!-- 指定的provider上面不再运行默认模块 -->
	<ProviderNoDefaultModule providers="0,1,2"/>
</ProviderModuleBinds>
```
上面的配置表明providers=0，1，2只运行Zeze.Special模块。

## 动态模块说明
由于动态模块的选择provider行为是程序控制的，所以实际上它可以运行在所有可选的provider上。
这样它可以只选择哪些排除了默认模块的provider（通过ProviderNoDefaultModule），达到动态模块
只运行在特别的服务器上。
