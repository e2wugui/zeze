# Misc

## Zeze.Services.ReloadClassServer
1. 构造服务
‘’‘
	/**
	 * 构造ReloadClassServer
	 *
	 * @param app         应用App实例
	 * @param urlPath     上传文件的urlPath
	 * @param uploadDir   上传文件的目录
	 * @param fileVarName 上传文件名查询Key，
	 *                    比如直接上传的url:/upload?fileVarName=xxx
	 *                    或者表单中<input type="file" name="fileVarName" multiple>
	 */
	public ReloadClassServer(@NotNull AppBase app, @NotNull String urlPath, @NotNull String uploadDir, @NotNull String fileVarName)
	// 这个服务依赖App.getHttpServer()，需要App定义并初始化HttpServer。
’‘’
2. 在App里面定义变量：private ReloadClassServer reloadClassServer;
3. 构造并启动
'''
reloadClassServer = new ReloadClassServer(this, "/reloadClass", "upload", "filename");
reloadClassServer.start();
'''
4. uploadDir是存放上传zip文件的目录，程序启动会自动装载（再次ReloadClass以保持最新）。
5. ReloadClass本身没有版本管理支持，除了把上传的zip保存并在重启是再次Reload，没有其他支持了。
建议patch的管理方式是：程序发布以后，所有的ReloadClass放在本地一个目录地下，每次Reload时，
全部打包成zip上传。程序更新再次发布以后，清空本地目录，并且删除服务器的uploadDir下的zip文件。

## Zeze.Services.RunClassServer
1. 构造服务
'''
	/**
	 * 构造ReloadClassServer
	 *
	 * @param app         应用App实例
	 * @param urlPath     上传文件的urlPath
	 * @param uploadDir   上传文件的目录
	 * @param fileVarName 上传文件名查询Key，
	 *                    比如直接上传的url:/upload?fileVarName=xxx
	 *                    或者表单中<input type="file" name="fileVarName" multiple>
	 */
	public RunClassServer(@NotNull AppBase app, @NotNull String urlPath, @NotNull String uploadDir, @NotNull String fileVarName)
'''
2. 在App里面定义变量：private RunClassServer runClassServer;
3. 构造初始化
'''
runClassServer = new RunClassServer(this, "/runClass", "clazz", "filename");
'''
4. uploadDir是存放上传的class的目录，重启不会再次运行。
5. RunClass每次上传都执行一次，必须保持Classs文件名是新的，重名的class不会重新装载新的，会执行老的class。覆盖热更see ReloadClass。
