#pragma once

#include "zeze/cxx/Bean.h"

namespace demo {
// bean的注释
class Bean1 : public Zeze::Bean {
public:
    static const int64_t TYPEID = -410057899348847631LL;

    static const int Enum1 = 4; // enum的注释

    int V1; // bean1comm		bean1line2
    std::map<int, int> V2; // bean1v2		bean1v2line2

    Bean1();
    Bean1(int V1_);
    virtual void Assign(const Zeze::Bean& other) override;
    void Assign(const Bean1& other);
    Bean1& operator=(const Bean1& other);
    virtual int64_t TypeId() const override {
        return TYPEID;
    }

    virtual void Encode(Zeze::ByteBuffer& _o_) const override;
    void Decode(Zeze::ByteBuffer& _o_) override;
};
}
