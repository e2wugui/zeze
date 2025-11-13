
Net
	1. 现在网络接口很简单，可能未来需要写一个比较完整的版本。
	    【不要大用特用】
	2. Protocol 生命期
	    自行管理生命期，一般建议使用栈变量。【内部不管生命期】发完了事。
	3. Rpc 生命期
	    a) 同步模式
	    自行管理生命期，使用栈变量或new（自己delete）都可以。
	    等待到结果以后继续处理。例子：
	    Rpc1 rpc1;
	    rpc1.SendForWait(socket)->Wait();
	    rpc1.Result Process;
	    b) 异步模式
	    必须new出来，例子如下：
	    (new Rpc1())->SendAsync(socket, []() { process async response ...; return 0; });
	    【SendAsync接管生命期管理】

	4. 重载 Service DispatchProtocol DispatchRpcResponse 时对传入Protocol*p的生命期管理
	    【Dispatch时需要管理传入的*p】
	   例子：
	void Service::DispatchProtocol(Protocol* p, Service::ProtocolFactoryHandle& factoryHandle)
	{
		std::auto_ptr<Protocol> at(p);
		factoryHandle.Handle(p); // 马上处理，处理完，auto_ptr会释放内存，
		// 如果Dispatch把p传给其他地方而不是马上处理，那么接收的地方获得p的所有权。
		// 比如传给main线程的队列。main_thread_task_queue.add(at.release());
	}

	void Service::DispatchRpcResponse(Protocol* r, std::function<int(Protocol*)>& responseHandle, Service::ProtocolFactoryHandle& factoryHandle)
	{
		std::auto_ptr<Protocol> at(r);
		responseHandle(r);
		// 同 DispatchProtocol
	}

	* Protocol * Rpc * 处理函数一般不delete，基本规则是交给dispatch管理。
	   但是在实现dispatch时，自己也可以不管理，process的最后加一个delete。
	  【但是不建议这样】。

Module & Gen
	0. cxx的模块定义方式和其他语言版本差不多。
	   和服务器（如java）共享solution.xml。
	   增加一个project生成cxx的代码模板。
	1. App 定义所有的模块和服务，App本身是一个单件。通过App::GetInstance()得到引用。
	2. Module cxx 版本生成.h,.cpp两个文件，协议处理的实现写到cpp中。
	    这两个文件生成到projectName/namespace下，属于srcDir。
	3. Service.h 生成到srcDir的solutionName/下面，一般来说需要重载
                 DispatchProtocol DispatchRpcResponse两个方法，用来控制协议在客户端框架中的哪个线程来执行。
                 默认是在网络线程中执行，这对于客户端框架一般是不合适的。
	4. projectName/Gen 这是生成文件目录，总是覆盖的文件。包括Bean，模块的AbstractModule.h;.cpp，App.h等等。
