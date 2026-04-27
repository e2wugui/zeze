// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

// https://astro.build/config
export default defineConfig({
	site: 'https://e2wugui.github.io',
	base: '/zeze',
	trailingSlash: 'always',
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
					autogenerate: { directory: 'getting-started' },
				},
				{
					label: '核心概念',
					autogenerate: { directory: 'core' },
				},
				{
					label: '架构',
					autogenerate: { directory: 'architecture' },
				},
				{
					label: '组件',
					autogenerate: { directory: 'components' },
				},
				{
					label: '服务',
					autogenerate: { directory: 'services' },
				},
				{
					label: '游戏模块',
					autogenerate: { directory: 'game' },
				},
				{
					label: '数据库',
					autogenerate: { directory: 'database' },
				},
				{
					label: '运维与配置',
					autogenerate: { directory: 'devops' },
				},
				{
					label: '进阶',
					autogenerate: { directory: 'advanced' },
				},
				{
					label: '其他',
					autogenerate: { directory: 'other' },
				},
				{
					label: '草稿',
					autogenerate: { directory: 'draft' },
				},
			],
		}),
	],
});
