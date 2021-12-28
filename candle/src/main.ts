import { createApp } from 'vue'
import { createPinia } from "pinia";
import { VuesticPlugin } from 'vuestic-ui'
import 'vuestic-ui/dist/vuestic-ui.css'
import App from './App.vue'

createApp(App)
    .use(createPinia())
    .use(VuesticPlugin)
    .mount('#app')
