#pragma once

namespace limax {

	class DHContext
	{
	public:
		DHContext();
		virtual ~DHContext();
	public:
		virtual const std::vector<unsigned char>& generateDHResponse() = 0;
		virtual const std::vector<unsigned char>& computeDHKey(unsigned char* response, int32_t size) = 0;
	};

	std::shared_ptr<DHContext> createDHContext(int group);

} // namespace limax {
