// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

// https://astro.build/config
export default defineConfig({
	site: 'https://e2wugui.github.io',
	base: '/zeze/',
	integrations: [
		starlight({
			title: 'Zeze 文档',
			defaultLocale: 'zh',
			social: [{ icon: 'github', label: 'GitHub', href: 'https://github.com/e2wugui/zeze' }],
			editLink: {
				baseUrl: 'https://github.com/e2wugui/zeze/edit/master/docs/'
			},
			sidebar: [
				{
					label: '入门指南',
					items: [
						{ slug: 'getting-started/preface', label: '前言' },
						{ slug: 'getting-started/what-is-zeze', label: 'What Is Zeze' },
						{ slug: 'getting-started/why-zeze', label: 'Why Zeze' },
						{ slug: 'getting-started/theory', label: '理论基础' },
						{ slug: 'getting-started/quick-start', label: '快速开始' },
					],
				},
				{
					label: '核心概念',
					items: [
						{ slug: 'core/transaction', label: '事务' },
						{ slug: 'core/solution-xml', label: 'Solution.xml 配置' },
						{ slug: 'core/third-party-interactions', label: '事务与第三方交互' },
						{ slug: 'core/listener', label: 'Listener' },
						{ slug: 'core/bean', label: 'Bean 数据结构' },
						{ slug: 'core/serialize', label: '序列化' },
					],
				},
				{
					label: '架构',
					items: [
						{ slug: 'architecture/arch', label: 'Provider-Linkd 架构' },
						{ slug: 'architecture/net', label: '网络层' },
						{ slug: 'architecture/one-world-one-dream', label: '全球同服' },
						{ slug: 'architecture/providers', label: 'Provider 模块绑定' },
					],
				},
				{
					label: '组件',
					items: [
						{ slug: 'components/overview', label: '组件概览' },
						{ slug: 'components/collections', label: 'Collections 总览' },
						{ slug: 'components/bool-list', label: 'BoolList' },
						{ slug: 'components/chashmap', label: 'CHashMap' },
						{ slug: 'components/cs-queue', label: 'CsQueue' },
						{ slug: 'components/department-tree', label: 'DepartmentTree' },
						{ slug: 'components/linked-map', label: 'LinkedMap' },
						{ slug: 'components/queue', label: 'Queue' },
						{ slug: 'components/signal', label: 'Signal' },
					],
				},
				{
					label: '服务',
					items: [
						{ slug: 'services/services', label: '内置服务' },
						{ slug: 'services/raft', label: 'Raft 共识' },
						{ slug: 'services/web-netty', label: 'Web (Netty)' },
						{ slug: 'services/platform', label: '多平台支持' },
					],
				},
				{
					label: '游戏模块',
					items: [
						{ slug: 'game/game', label: '游戏模块概览' },
						{ slug: 'game/timer', label: '定时器' },
						{ slug: 'game/task', label: '任务系统' },
						{ slug: 'game/login-queue', label: '登录队列' },
						{ slug: 'game/wss', label: 'WebSocket' },
					],
				},
				{
					label: '数据库',
					items: [
						{ slug: 'database/dbh2', label: 'Dbh2' },
						{ slug: 'database/tikv', label: 'TiKV' },
					],
				},
				{
					label: '运维与配置',
					items: [
						{ slug: 'devops/devops-and-configuration', label: '运维与配置' },
						{ slug: 'devops/custom-configuration', label: '自定义配置' },
						{ slug: 'devops/log', label: '日志' },
						{ slug: 'devops/limit', label: '系统限制' },
						{ slug: 'devops/maven-deploy', label: 'Maven 部署' },
						{ slug: 'devops/release-and-update', label: '发布与热更' },
						{ slug: 'devops/reload-class', label: 'ReloadClass & RunClass' },
					],
				},
				{
					label: '进阶',
					items: [
						{ slug: 'advanced/inside-zeze', label: 'Zeze 内部实现' },
						{ slug: 'advanced/onz', label: 'Onz 框架' },
						{ slug: 'advanced/performance', label: '性能' },
						{ slug: 'advanced/threads', label: '线程模型' },
					],
				},
				{
					label: '其他',
					items: [
						{ slug: 'other/history', label: '项目历史' },
						{ slug: 'other/git', label: 'Git 使用' },
						{ slug: 'other/misc', label: '杂项' },
						{ slug: 'other/suggestion', label: '建议' },
						{ slug: 'other/others', label: '其他' },
					],
				},
			],
		}),
	],
});
