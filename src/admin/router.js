// 管理端路由：/admin.html#/overview 等子路由（hash 模式，免服务器 fallback 配置）
import { createRouter, createWebHashHistory } from 'vue-router'
import OverviewView from './views/OverviewView.vue'
import MaterialsView from './views/MaterialsView.vue'
import UsersView from './views/UsersView.vue'
import InspirationsView from './views/InspirationsView.vue'
import OrdersView from './views/OrdersView.vue'

export default createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: { name: 'overview' } },
    { path: '/overview', name: 'overview', component: OverviewView },
    { path: '/materials', name: 'materials', component: MaterialsView },
    { path: '/inspirations', name: 'inspirations', component: InspirationsView },
    { path: '/users', name: 'users', component: UsersView },
    { path: '/orders', name: 'orders', component: OrdersView },
  ],
})
