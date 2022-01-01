import { createApp } from 'vue'
import { createPinia } from "pinia";
import "bootstrap/dist/css/bootstrap.min.css"
import "bootstrap"
import App from './App.vue'

createApp(App)
    .use(createPinia())
    .mount('#app')
