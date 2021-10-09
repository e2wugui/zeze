package Zeze.Gen;

import Zeze.*;
import java.util.*;

public class Protocol {
	private ModuleSpace Space;
	public final ModuleSpace getSpace() {
		return Space;
	}
	private void setSpace(ModuleSpace value) {
		Space = value;
	}
	private String Name;
	public final String getName() {
		return Name;
	}
	private void setName(String value) {
		Name = value;
	}

	public final String ShortNameIf(ModuleSpace holder) {
		return holder == getSpace() ? getName() : getFullName();
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private ushort Id;
	private short Id;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public ushort getId()
	public final short getId() {
		return Id;
	}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private void setId(ushort value)
	private void setId(short value) {
		Id = value;
	}
	public final int getTypeId() {
		return ((int)getSpace().getId() << 16) | ((int)getId() & 0xffff);
	}
	private String Argument;
	public final String getArgument() {
		return Argument;
	}
	private void setArgument(String value) {
		Argument = value;
	}
	private String Handle;
	public final String getHandle() {
		return Handle;
	}
	private void setHandle(String value) {
		Handle = value;
	}
	private int HandleFlags;
	public final int getHandleFlags() {
		return HandleFlags;
	}
	private boolean NoProcedure;
	public final boolean getNoProcedure() {
		return NoProcedure;
	}
	private ArrayList<Zeze.Gen.Types.Enum> Enums = new ArrayList<Zeze.Gen.Types.Enum> ();
	public final ArrayList<Zeze.Gen.Types.Enum> getEnums() {
		return Enums;
	}
	private void setEnums(ArrayList<Zeze.Gen.Types.Enum> value) {
		Enums = value;
	}
	public final String getFullName() {
		return getSpace().Path(".", getName());
	}

	// setup in compile
	private Zeze.Gen.Types.Type ArgumentType;
	public final Zeze.Gen.Types.Type getArgumentType() {
		return ArgumentType;
	}
	private void setArgumentType(Zeze.Gen.Types.Type value) {
		ArgumentType = value;
	}

	public Protocol(ModuleSpace space, XmlElement self) {
		setSpace(space);
		setName(self.GetAttribute("name").strip());
		space.Add(this);

		String attr = self.GetAttribute("id");
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Id = attr.Length > 0 ? ushort.Parse(attr) : Zeze.Transaction.Bean.Hash16(space.Path(".", Name));
		setId(attr.length() > 0 ? Short.parseShort(attr) : Zeze.Transaction.Bean.Hash16(space.Path(".", getName())));
		space.getProtocolIdRanges().CheckAdd(getId());

		setArgument(self.GetAttribute("argument"));
		setHandle(self.GetAttribute("handle"));
		HandleFlags = Program.ToHandleFlags(getHandle());
		NoProcedure = "true".equals(self.GetAttribute("NoProcedure"));

		XmlNodeList childNodes = self.ChildNodes;
		for (XmlNode node : childNodes) {
			if (XmlNodeType.Element != node.NodeType) {
				continue;
			}

			XmlElement e = (XmlElement)node;

			String nodename = e.Name;
			switch (e.Name) {
				case "enum":
					Add(new Zeze.Gen.Types.Enum(e));
					break;
				default:
					throw new RuntimeException("node=" + nodename);
			}
		}
	}

	public final void Add(Zeze.Gen.Types.Enum e) {
		getEnums().add(e); // check duplicate
	}

	public void Compile() {
		setArgumentType(getArgument().length() > 0 ? Types.Type.Compile(getSpace(), getArgument()) : null);
	}

	public void Depends(HashSet<Zeze.Gen.Types.Type> depends) {
		if (getArgumentType() != null) {
			getArgumentType().Depends(depends);
		}
	}
}