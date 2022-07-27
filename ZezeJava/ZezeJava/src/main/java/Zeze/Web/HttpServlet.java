package Zeze.Web;

import java.nio.charset.StandardCharsets;
import Zeze.Builtin.Web.RequestJson;
import Zeze.Builtin.Web.RequestQuery;
import Zeze.Net.Binary;

public abstract class HttpServlet {

	public boolean handle(Web web, RequestJson r) throws Throwable {
		r.Result.setContentType("text/plain; charset=utf-8");
		r.Result.setBody(new Binary("Not Implement.".getBytes(StandardCharsets.UTF_8)));
		r.SendResult();
		return false;
	}

	public boolean handle(Web web, RequestQuery r) throws Throwable {
		r.Result.setContentType("text/plain; charset=utf-8");
		r.Result.setBody(new Binary("Not Implement.".getBytes(StandardCharsets.UTF_8)));
		r.SendResult();
		return false;
	}
}
