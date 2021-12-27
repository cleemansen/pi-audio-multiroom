import { createApp } from 'vue'
import Antd from 'ant-design-vue'
import "ant-design-vue/dist/antd.css";
import { createPinia } from "pinia";
import App from './App.vue'

createApp(App)
    .use(createPinia())
    .use(Antd)
    .mount('#app')
