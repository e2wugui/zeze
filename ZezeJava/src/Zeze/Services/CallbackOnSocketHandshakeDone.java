package Zeze.Services;

import Zeze.*;
import java.util.*;

//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if USE_UNITY_PUERTS
// 下面这个类是真正开放给ts用的，使用Puerts绑定，需要Puerts支持。
// 使用的时候拷贝下面的代码到你自己的ToTypeScriptService.cs文件。
// 并且在Puerts.Binding里面增加 typeof 绑定到ts。

@FunctionalInterface
public interface CallbackOnSocketHandshakeDone {
	void invoke(long sessionId);
}