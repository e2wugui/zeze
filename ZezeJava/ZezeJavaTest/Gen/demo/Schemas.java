// auto-generated @formatter:off
package demo;

public class Schemas extends Zeze.Schemas {
    public Schemas() {
        addTable(new Zeze.Schemas.Table("demo_Game_tMail", "long", "demo.Game.BMail", 0));
        addTable(new Zeze.Schemas.Table("demo_Game_tRole", "long", "demo.Game.BRole", 0));
        addTable(new Zeze.Schemas.Table("demo_Game_tSession", "long", "demo.Game.BSession", 0));
        addTable(new Zeze.Schemas.Table("demo_Module1_Table1", "long", "demo.Module1.BValue", 0));
        addTable(new Zeze.Schemas.Table("demo_Module1_Table2", "demo.Module1.Key", "demo.Module1.BValue", 0));
        addTable(new Zeze.Schemas.Table("demo_Module1_Table3", "long", "demo.Module1.BValue", 0));
        addTable(new Zeze.Schemas.Table("demo_Module1_Table4", "long", "demo.Module1.BValue", 0));
        addTable(new Zeze.Schemas.Table("demo_Module1_TableImportant", "long", "demo.Module1.BValue", 0));
        addTable(new Zeze.Schemas.Table("demo_Module1_tAutokey", "demo.Module1.AutoKey", "demo.Module1.BAutoValue", 0));
        addTable(new Zeze.Schemas.Table("demo_Module1_tAutoKeyRandom", "binary", "demo.Bean1", 0));
        addTable(new Zeze.Schemas.Table("demo_Module1_tCoverHistory", "long", "demo.Module1.BValue", 0));
        addTable(new Zeze.Schemas.Table("demo_Module1_tflush", "long", "demo.Module1.BValue", 0));
        addTable(new Zeze.Schemas.Table("demo_Module1_tKuafu", "long", "demo.Module1.BKuafu", 0));
        addTable(new Zeze.Schemas.Table("demo_Module1_tMemorySize", "long", "demo.Bean1", 0));
        addTable(new Zeze.Schemas.Table("demo_Module1_tTestSchemas", "long", "demo.Module1.BTestSchemas", 0));
        addTable(new Zeze.Schemas.Table("demo_Module1_tWalkPage", "int", "demo.Bean1", 0));
        addTable(new Zeze.Schemas.Table("demo_ModuleGTable_tGTable", "long", "demo.ModuleGTable.BValue", 0));
        addTable(new Zeze.Schemas.Table("demo_ModuleGTable_tGTable2", "long", "demo.ModuleGTable.BValue2", 0));
        addTable(new Zeze.Schemas.Table("demo_web_tHelloCount", "long", "demo.web.BCount", 0));
        addTable(new Zeze.Schemas.Table("demo_web_tMap2Bean1", "long", "demo.web.BMap2Bean1", 0));
        addBean_demo_Game_BMail();
        addBean_demo_Game_BItem();
        addBean_demo_Game_BRole();
        addBean_demo_Game_BSession();
        addBean_demo_Module1_BValue();
        addBean_demo_Bean1();
        addBean_demo_Module2_BValue();
        addBean_demo_Module1_BSimple();
        addBean_demo_Module1_BRemoved2();
        addBean_demo_Module1_BItem();
        addBean_demo_Module1_BFood();
        addBean_demo_Module1_Key();
        addBean_demo_Module1_AutoKey();
        addBean_demo_Module1_BAutoValue();
        addBean_demo_Module1_BKuafu();
        addBean_demo_Module1_BTestSchemas();
        addBean_demo_ModuleGTable_BValue();
        addBean_demo_ModuleGTable_Bean1();
        addBean_demo_ModuleGTable_BValue2();
        addBean_demo_web_BCount();
        addBean_demo_web_BMap2Bean1();
    }

    private void addBean_demo_Game_BMail() {
        var bean = new Zeze.Schemas.Bean("demo.Game.BMail", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "From";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "Money";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 3;
            var.name = "Items";
            var.typeName = "map";
            var.keyName = "int";
            var.valueName = "demo.Game.BItem";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 4;
            var.name = "Claimed";
            var.typeName = "bool";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_demo_Game_BItem() {
        var bean = new Zeze.Schemas.Bean("demo.Game.BItem", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "ItemId";
            var.typeName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "Count";
            var.typeName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 3;
            var.name = "Atk";
            var.typeName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 4;
            var.name = "Def";
            var.typeName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 5;
            var.name = "AddHp";
            var.typeName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 6;
            var.name = "Price";
            var.typeName = "int";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_demo_Game_BRole() {
        var bean = new Zeze.Schemas.Bean("demo.Game.BRole", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "Level";
            var.typeName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "Exp";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 3;
            var.name = "Money";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 4;
            var.name = "Hp";
            var.typeName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 5;
            var.name = "DamageTotal";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 6;
            var.name = "Bag";
            var.typeName = "map";
            var.keyName = "int";
            var.valueName = "demo.Game.BItem";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 7;
            var.name = "Equip";
            var.typeName = "map";
            var.keyName = "int";
            var.valueName = "demo.Game.BItem";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 8;
            var.name = "Friends";
            var.typeName = "map";
            var.keyName = "long";
            var.valueName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 9;
            var.name = "Tracks";
            var.typeName = "list";
            var.valueName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 10;
            var.name = "QuestLog";
            var.typeName = "list";
            var.valueName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 11;
            var.name = "GiftLog";
            var.typeName = "list";
            var.valueName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 12;
            var.name = "Warehouse";
            var.typeName = "list";
            var.valueName = "demo.Game.BItem";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_demo_Game_BSession() {
        var bean = new Zeze.Schemas.Bean("demo.Game.BSession", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "LastActive";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "RoomId";
            var.typeName = "int";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_demo_Module1_BValue() {
        var bean = new Zeze.Schemas.Bean("demo.Module1.BValue", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "int_1";
            var.typeName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "long2";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 3;
            var.name = "string3";
            var.typeName = "string";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 4;
            var.name = "bool4";
            var.typeName = "bool";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 5;
            var.name = "short5";
            var.typeName = "short";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 6;
            var.name = "float6";
            var.typeName = "float";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 7;
            var.name = "double7";
            var.typeName = "double";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 8;
            var.name = "bytes8";
            var.typeName = "binary";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 9;
            var.name = "list9";
            var.typeName = "list";
            var.valueName = "demo.Bean1";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 10;
            var.name = "set10";
            var.typeName = "set";
            var.valueName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 11;
            var.name = "map11";
            var.typeName = "map";
            var.keyName = "long";
            var.valueName = "demo.Module2.BValue";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 12;
            var.name = "bean12";
            var.typeName = "demo.Module1.BSimple";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 13;
            var.name = "byte13";
            var.typeName = "byte";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 14;
            var.name = "dynamic14";
            var.typeName = "dynamic";
            var.dynamicBeans.put(1L, "demo.Bean1");
            var.dynamicBeans.put(2L, "demo.Module1.BSimple");
            var.dynamicBeans.put(3L, "demo.Module1.BItem");
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 15;
            var.name = "map15";
            var.typeName = "map";
            var.keyName = "long";
            var.valueName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 16;
            var.name = "map16";
            var.typeName = "map";
            var.keyName = "demo.Module1.Key";
            var.valueName = "demo.Module1.BSimple";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 17;
            var.name = "vector2";
            var.typeName = "vector2";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 18;
            var.name = "vector2Int";
            var.typeName = "vector2int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 19;
            var.name = "vector3";
            var.typeName = "vector3";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 20;
            var.name = "vector3Int";
            var.typeName = "vector3int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 21;
            var.name = "vector4";
            var.typeName = "vector4";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 22;
            var.name = "quaternion";
            var.typeName = "quaternion";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 23;
            var.name = "dynamic23";
            var.typeName = "dynamic";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 24;
            var.name = "listVector2Int";
            var.typeName = "list";
            var.valueName = "vector2int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 25;
            var.name = "map25";
            var.typeName = "map";
            var.keyName = "demo.Module1.Key";
            var.valueName = "demo.Module1.BSimple";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 26;
            var.name = "map26";
            var.typeName = "map";
            var.keyName = "demo.Module1.Key";
            var.valueName = "dynamic";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 27;
            var.name = "dynamic27";
            var.typeName = "dynamic";
            var.dynamicBeans.put(4513771153805810055L, "demo.Module1.BSimple");
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 28;
            var.name = "key28";
            var.typeName = "demo.Module1.Key";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 29;
            var.name = "array29";
            var.typeName = "array";
            var.valueName = "float";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 30;
            var.name = "list30";
            var.typeName = "list";
            var.valueName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 31;
            var.name = "list31";
            var.typeName = "list";
            var.valueName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 32;
            var.name = "list32";
            var.typeName = "list";
            var.valueName = "float";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 33;
            var.name = "list33";
            var.typeName = "list";
            var.valueName = "vector2";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 34;
            var.name = "list34";
            var.typeName = "list";
            var.valueName = "vector3";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 35;
            var.name = "list35";
            var.typeName = "list";
            var.valueName = "vector4";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 36;
            var.name = "list36";
            var.typeName = "list";
            var.valueName = "vector2int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 37;
            var.name = "list37";
            var.typeName = "list";
            var.valueName = "vector3int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 38;
            var.name = "set38";
            var.typeName = "set";
            var.valueName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 39;
            var.name = "set39";
            var.typeName = "set";
            var.valueName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 40;
            var.name = "map40";
            var.typeName = "map";
            var.keyName = "int";
            var.valueName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 41;
            var.name = "map41";
            var.typeName = "map";
            var.keyName = "long";
            var.valueName = "demo.Module1.BSimple";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 42;
            var.name = "map42Recursive";
            var.typeName = "map";
            var.keyName = "long";
            var.valueName = "demo.Module1.BValue";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 43;
            var.name = "list43";
            var.typeName = "list";
            var.valueName = "dynamic";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 44;
            var.name = "jsonObject";
            var.typeName = "string";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 45;
            var.name = "jsonArray";
            var.typeName = "string";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 46;
            var.name = "relationalMappingAlter";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 47;
            var.name = "sortedmap1";
            var.typeName = "sortedmap";
            var.keyName = "int";
            var.valueName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 48;
            var.name = "sortedmap2";
            var.typeName = "sortedmap";
            var.keyName = "int";
            var.valueName = "demo.Module1.BSimple";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 50;
            var.name = "version";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 99;
            var.name = "LongList";
            var.typeName = "list";
            var.valueName = "long";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_demo_Bean1() {
        var bean = new Zeze.Schemas.Bean("demo.Bean1", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "V1";
            var.typeName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "V2";
            var.typeName = "map";
            var.keyName = "int";
            var.valueName = "int";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_demo_Module2_BValue() {
        var bean = new Zeze.Schemas.Bean("demo.Module2.BValue", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "s";
            var.typeName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "plist";
            var.typeName = "list";
            var.valueName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 3;
            var.name = "psortedmap";
            var.typeName = "sortedmap";
            var.keyName = "int";
            var.valueName = "int";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_demo_Module1_BSimple() {
        var bean = new Zeze.Schemas.Bean("demo.Module1.BSimple", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "int_1";
            var.typeName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "long2";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 3;
            var.name = "string3";
            var.typeName = "string";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 4;
            var.name = "removed";
            var.typeName = "demo.Module1.BRemoved2";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_demo_Module1_BRemoved2() {
        var bean = new Zeze.Schemas.Bean("demo.Module1.BRemoved2", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "int_1";
            var.typeName = "int";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_demo_Module1_BItem() {
        var bean = new Zeze.Schemas.Bean("demo.Module1.BItem", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 14;
            var.name = "Subclass";
            var.typeName = "dynamic";
            var.dynamicBeans.put(1L, "demo.Bean1");
            var.dynamicBeans.put(2L, "demo.Module1.BSimple");
            var.dynamicBeans.put(3L, "demo.Module1.BFood");
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_demo_Module1_BFood() {
        var bean = new Zeze.Schemas.Bean("demo.Module1.BFood", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 14;
            var.name = "Subclass";
            var.typeName = "dynamic";
            var.dynamicBeans.put(1L, "demo.Bean1");
            var.dynamicBeans.put(2L, "demo.Module1.BSimple");
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_demo_Module1_Key() {
        var bean = new Zeze.Schemas.Bean("demo.Module1.Key", true);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "s";
            var.typeName = "short";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "str";
            var.typeName = "string";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_demo_Module1_AutoKey() {
        var bean = new Zeze.Schemas.Bean("demo.Module1.AutoKey", true);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "Name";
            var.typeName = "string";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "LocalId";
            var.typeName = "long";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_demo_Module1_BAutoValue() {
        var bean = new Zeze.Schemas.Bean("demo.Module1.BAutoValue", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "Current";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "Name";
            var.typeName = "string";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 3;
            var.name = "LocalId";
            var.typeName = "long";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_demo_Module1_BKuafu() {
        var bean = new Zeze.Schemas.Bean("demo.Module1.BKuafu", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "Account";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "Money";
            var.typeName = "long";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_demo_Module1_BTestSchemas() {
        var bean = new Zeze.Schemas.Bean("demo.Module1.BTestSchemas", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "Var1";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "Var2";
            var.typeName = "long";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_demo_ModuleGTable_BValue() {
        var bean = new Zeze.Schemas.Bean("demo.ModuleGTable.BValue", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "gTable";
            var.typeName = "gtable";
            var.keyName = "int,int";
            var.valueName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "gTable2";
            var.typeName = "gtable";
            var.keyName = "int,string";
            var.valueName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 3;
            var.name = "gTable3";
            var.typeName = "gtable";
            var.keyName = "int,int";
            var.valueName = "dynamic";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 4;
            var.name = "gTable4";
            var.typeName = "gtable";
            var.keyName = "int,string";
            var.valueName = "dynamic";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_demo_ModuleGTable_Bean1() {
        var bean = new Zeze.Schemas.Bean("demo.ModuleGTable.Bean1", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "IntVar";
            var.typeName = "int";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_demo_ModuleGTable_BValue2() {
        var bean = new Zeze.Schemas.Bean("demo.ModuleGTable.BValue2", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "gTable";
            var.typeName = "gtable";
            var.keyName = "int,int";
            var.valueName = "demo.ModuleGTable.Bean1";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_demo_web_BCount() {
        var bean = new Zeze.Schemas.Bean("demo.web.BCount", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "Count";
            var.typeName = "long";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_demo_web_BMap2Bean1() {
        var bean = new Zeze.Schemas.Bean("demo.web.BMap2Bean1", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "map2";
            var.typeName = "map";
            var.keyName = "int";
            var.valueName = "demo.Bean1";
            bean.addVariable(var);
        }
        addBean(bean);
    }
}
