#pragma once

#include "zeze/cxx/Bean.h"

namespace TaskTest {
namespace TaskExt {
// 扩展的任务 Extended Task
class BMyTask01 : public Zeze::Bean {
public:
    static const int64_t TYPEID = -3524416178202009108LL;

    int64_t Placeholder;
    int TaskNum;

    BMyTask01();
    BMyTask01(int64_t Placeholder_, int TaskNum_);
    virtual void Assign(const Zeze::Bean& other) override;
    void Assign(const BMyTask01& other);
    BMyTask01& operator=(const BMyTask01& other);
    virtual int64_t TypeId() const override {
        return TYPEID;
    }

    virtual void Encode(Zeze::ByteBuffer& _o_) const override;
    void Decode(Zeze::ByteBuffer& _o_) override;
};
}
}
