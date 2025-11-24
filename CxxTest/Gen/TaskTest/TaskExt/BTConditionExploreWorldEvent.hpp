#pragma once

#include "zeze/cxx/Bean.h"

namespace TaskTest {
namespace TaskExt {
class BTConditionExploreWorldEvent : public Zeze::Bean {
public:
    static const int64_t TYPEID = 9166177412975401838LL;

    double ExploreRate; // 大地图探索率：0 - 100

    BTConditionExploreWorldEvent();
    BTConditionExploreWorldEvent(double ExploreRate_);
    virtual void Assign(const Zeze::Bean& other) override;
    void Assign(const BTConditionExploreWorldEvent& other);
    BTConditionExploreWorldEvent& operator=(const BTConditionExploreWorldEvent& other);
    virtual int64_t TypeId() const override {
        return TYPEID;
    }

    virtual void Encode(Zeze::ByteBuffer& _o_) const override;
    void Decode(Zeze::ByteBuffer& _o_) override;
};
}
}
