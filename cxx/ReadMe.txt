使用(lua)
	依赖lualib
		需要设置includepath
	直接把cxx下的所有代码加到项目中。
	除了ToTypeScript相关的。

使用(TypeScript)
	1 直接把cxx下的所有代码加到项目中。除了ToLua相关的。
	2 把 TypeScript/scripts/zeze.ts 或者 TypeScript/js/zeze.js 拷贝到你的项目脚本目录。
	  TypeScript/js/long.js 来自于 https://github.com/dcodeIO/Long.js/ 
	  好像 nodejs 也有一个 Long 实现，还没看，不知道接口是否一致。


其他说明
	这里的代码主要拷贝自 limax 项目。
	主页地址：http://www.limax-project.org/
	包含加密压缩，dh交换等功能。
	除了下面的代码
	ByteBuffer：Zeze 系列化 cxx 实现
	Net：一个仅支持client的连接实现
	Protocol：Zeze 协议基类和解码实现
	ToLua：把协议映射到lua的实现
	ToTypeScript：把网络绑定到 ts 中。
