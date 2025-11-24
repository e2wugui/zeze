#pragma once

#include "zeze/cxx/Bean.h"

namespace TaskTest {
namespace TaskExt {
// 扩展的条件 Extended Condition
class BTConditionExploreWorld : public Zeze::Bean {
public:
    static const int64_t TYPEID = 5234468502297408856LL;

    double ExploreRate; // 大地图探索率：0 - 100
    bool Finished;

    BTConditionExploreWorld();
    BTConditionExploreWorld(double ExploreRate_, bool Finished_);
    virtual void Assign(const Zeze::Bean& other) override;
    void Assign(const BTConditionExploreWorld& other);
    BTConditionExploreWorld& operator=(const BTConditionExploreWorld& other);
    virtual int64_t TypeId() const override {
        return TYPEID;
    }

    virtual void Encode(Zeze::ByteBuffer& _o_) const override;
    void Decode(Zeze::ByteBuffer& _o_) override;
};
}
}
