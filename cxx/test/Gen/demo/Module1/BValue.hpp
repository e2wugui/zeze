#pragma once

#include "zeze/cxx/Bean.h"
#include "Gen/demo/Bean1.hpp"
#include "Gen/demo/Module2/BValue.hpp"
#include "Gen/demo/Module1/BSimple.hpp"
#include "Gen/demo/Module1/BItem.hpp"
#include "Gen/demo/Module1/Key.hpp"
#include "Gen/demo/Module1/BValue.hpp"

namespace demo {
namespace Module1 {
class BValue : public Zeze::Bean {
public:
    static const int64_t TYPEID = -9185494755353500202LL;

    static const int Enum1 = 4; // enum的注释

    int Int_1; // com aa
    int64_t Long2; // com aa
    std::string String3; // com aa
    bool Bool4; // com aa
    short Short5; // com aa
    float Float6; // com aa
    double Double7; // com aa
    std::string Bytes8; // com aa
    std::vector<demo::Bean1> List9; // com aa
    std::set<int> Set10; // com aa
    std::map<int64_t, demo::Module2::BValue> Map11; // com aa
    demo::Module1::BSimple Bean12; // simple
    char Byte13; // com aa
    Zeze::DynamicBean Dynamic14;
    static const int64_t DynamicTypeId_Dynamic14_demo_Bean1 = 1L;
    static const int64_t DynamicTypeId_Dynamic14_demo_Module1_BSimple = 2L;
    static const int64_t DynamicTypeId_Dynamic14_demo_Module1_BItem = 3L;

    static Zeze::DynamicBean constructDynamicBean_Dynamic14();
    static int64_t GetSpecialTypeIdFromBean_14(const Zeze::Bean* bean);
    static Zeze::Bean* CreateBeanFromSpecialTypeId_14(int64_t typeId);

    std::map<int64_t, int64_t> Map15; // com aa
    std::map<demo::Module1::Key, demo::Module1::BSimple> Map16; // com aa
    Zeze::Vector2 Vector2;
    Zeze::Vector2Int Vector2Int;
    Zeze::Vector3 Vector3;
    Zeze::Vector3Int Vector3Int;
    Zeze::Vector4 Vector4;
    Zeze::Quaternion Quaternion;
    Zeze::DynamicBean Dynamic23;

    static Zeze::DynamicBean constructDynamicBean_Dynamic23();
    static int64_t GetSpecialTypeIdFromBean_23(const Zeze::Bean* bean);
    static Zeze::Bean* CreateBeanFromSpecialTypeId_23(int64_t typeId);

    std::vector<Zeze::Vector2Int> ListVector2Int;
    std::map<demo::Module1::Key, demo::Module1::BSimple> Map25;
    std::map<demo::Module1::Key, Zeze::DynamicBean> Map26;
    static Zeze::DynamicBean constructDynamicBean_Map26();
    static int64_t GetSpecialTypeIdFromBean_26(const Zeze::Bean* bean);
    static Zeze::Bean* CreateBeanFromSpecialTypeId_26(int64_t typeId);

    Zeze::DynamicBean Dynamic27;
    static const int64_t DynamicTypeId_Dynamic27_demo_Module1_BSimple = 4513771153805810055L;

    static Zeze::DynamicBean constructDynamicBean_Dynamic27();
    static int64_t GetSpecialTypeIdFromBean_27(const Zeze::Bean* bean);
    static Zeze::Bean* CreateBeanFromSpecialTypeId_27(int64_t typeId);

    demo::Module1::Key Key28;
    std::vector<float> Array29;
    std::vector<int> List30;
    std::vector<int64_t> List31;
    std::vector<float> List32;
    std::vector<Zeze::Vector2> List33;
    std::vector<Zeze::Vector3> List34;
    std::vector<Zeze::Vector4> List35;
    std::vector<Zeze::Vector2Int> List36;
    std::vector<Zeze::Vector3Int> List37;
    std::set<int> Set38;
    std::set<int64_t> Set39;
    std::map<int, int> Map40;
    std::map<int64_t, demo::Module1::BSimple> Map41;
    std::map<int64_t, demo::Module1::BValue> Map42Recursive;
    std::vector<Zeze::DynamicBean> List43;
    static Zeze::DynamicBean constructDynamicBean_List43();
    static int64_t GetSpecialTypeIdFromBean_43(const Zeze::Bean* bean);
    static Zeze::Bean* CreateBeanFromSpecialTypeId_43(int64_t typeId);

    std::string JsonObject;
    std::string JsonArray;
    int64_t RelationalMappingAlter;
    int64_t Version;
    std::vector<int64_t> LongList;

    demo::Bean1* GetDynamic14_demo_Bean1() {
        return (demo::Bean1*)Dynamic14.GetBean();
    }

    void SetDynamic14(demo::Bean1* value) {
        Dynamic14.SetBean(value);
    }

    demo::Module1::BSimple* GetDynamic14_demo_Module1_BSimple() {
        return (demo::Module1::BSimple*)Dynamic14.GetBean();
    }

    void SetDynamic14(demo::Module1::BSimple* value) {
        Dynamic14.SetBean(value);
    }

    demo::Module1::BItem* GetDynamic14_demo_Module1_BItem() {
        return (demo::Module1::BItem*)Dynamic14.GetBean();
    }

    void SetDynamic14(demo::Module1::BItem* value) {
        Dynamic14.SetBean(value);
    }

    demo::Module1::BSimple* GetDynamic27_demo_Module1_BSimple() {
        return (demo::Module1::BSimple*)Dynamic27.GetBean();
    }

    void SetDynamic27(demo::Module1::BSimple* value) {
        Dynamic27.SetBean(value);
    }

    BValue();
    BValue(int Int_1_, int64_t Long2_, const std::string& String3_, bool Bool4_, short Short5_, float Float6_, double Double7_, const std::string& Bytes8_, char Byte13_, const Zeze::Vector2& Vector2_, const Zeze::Vector2Int& Vector2Int_, const Zeze::Vector3& Vector3_, const Zeze::Vector3Int& Vector3Int_, const Zeze::Vector4& Vector4_, const Zeze::Quaternion& Quaternion_, demo::Module1::Key Key28_, const std::string& JsonObject_, const std::string& JsonArray_, int64_t RelationalMappingAlter_);
    virtual void Assign(const Zeze::Bean& other) override;
    void Assign(const BValue& other);
    BValue& operator=(const BValue& other);
    virtual int64_t TypeId() const override {
        return TYPEID;
    }

    virtual void Encode(Zeze::ByteBuffer& _o_) const override;
    void Decode(Zeze::ByteBuffer& _o_) override;
};
}
}
