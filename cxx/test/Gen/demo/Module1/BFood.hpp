#pragma once

#include "zeze/cxx/Bean.h"
#include "Gen/demo/Bean1.hpp"
#include "Gen/demo/Module1/BSimple.hpp"

namespace demo {
namespace Module1 {
class BFood : public Zeze::Bean {
public:
    static const int64_t TYPEID = -3820308016141122965LL;

    Zeze::DynamicBean Subclass;
    static const int64_t DynamicTypeId_Subclass_demo_Bean1 = 1L;
    static const int64_t DynamicTypeId_Subclass_demo_Module1_BSimple = 2L;

    static Zeze::DynamicBean constructDynamicBean_Subclass();
    static int64_t GetSpecialTypeIdFromBean_14(const Zeze::Bean* bean);
    static Zeze::Bean* CreateBeanFromSpecialTypeId_14(int64_t typeId);

    demo::Bean1* GetSubclass_demo_Bean1() {
        return (demo::Bean1*)Subclass.GetBean();
    }

    void SetSubclass(demo::Bean1* value) {
        Subclass.SetBean(value);
    }

    demo::Module1::BSimple* GetSubclass_demo_Module1_BSimple() {
        return (demo::Module1::BSimple*)Subclass.GetBean();
    }

    void SetSubclass(demo::Module1::BSimple* value) {
        Subclass.SetBean(value);
    }

    BFood();
    virtual void Assign(const Zeze::Bean& other) override;
    void Assign(const BFood& other);
    BFood& operator=(const BFood& other);
    virtual int64_t TypeId() const override {
        return TYPEID;
    }

    virtual void Encode(Zeze::ByteBuffer& _o_) const override;
    void Decode(Zeze::ByteBuffer& _o_) override;
};
}
}
