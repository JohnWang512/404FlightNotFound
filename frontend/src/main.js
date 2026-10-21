// Entry point — creates the Vue app, registers the router, and mounts to #app in index.html
import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'

createApp(App).use(router).mount('#app')
