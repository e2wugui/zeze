package Zege;

import org.w3c.dom.Element;

public class ZegeConfig implements Zeze.Config.ICustomize {
	public int AboutHasRead = 3;
	public int AboutLast = 20;
	public int MessageLimit = 20;
	public int GroupChatLimit = 1000;
	public int DepartmentChildrenLimit = 300;
	public int GroupInviteLimit = 10;
	public int BelongDepartmentLimit = 100;

	@Override
	public String getName() {
		return "zege";
	}

	@Override
	public void Parse(Element self) {
		String attr;

		attr = self.getAttribute("AboutHasRead");
		if (!attr.isEmpty())
			AboutHasRead = Integer.parseInt(attr);

		attr = self.getAttribute("AboutLast");
		if (!attr.isEmpty())
			AboutLast = Integer.parseInt(attr);

		attr = self.getAttribute("MessageLimit");
		if (!attr.isEmpty())
			MessageLimit = Integer.parseInt(attr);

		attr = self.getAttribute("GroupChatLimit");
		if (!attr.isEmpty())
			GroupChatLimit = Integer.parseInt(attr);

		attr = self.getAttribute("DepartmentChildrenLimit");
		if (!attr.isEmpty())
			DepartmentChildrenLimit = Integer.parseInt(attr);

		attr = self.getAttribute("GroupInviteLimit");
		if (!attr.isEmpty())
			GroupInviteLimit = Integer.parseInt(attr);

		attr = self.getAttribute("BelongDepartmentLimit");
		if (!attr.isEmpty())
			BelongDepartmentLimit = Integer.parseInt(attr);
	}
}
