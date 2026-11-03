import { createApp } from 'vue'
import AdminApp from './AdminApp.vue'
import router from './router'

createApp(AdminApp).use(router).mount('#app')
