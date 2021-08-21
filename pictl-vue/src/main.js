import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import httpclient from '@/plugins/http-client'
import vuetify from './plugins/vuetify'
import webSocket from "@/plugins/webSocket"
import '@/mixins/helper'
import './registerServiceWorker'

Vue.config.productionTip = false

Vue.use(webSocket, {
  store,
  host: `wss://ktor.pictl.localhost`
})
Vue.use(httpclient)

new Vue({
  router,
  store,
  vuetify,
  render: h => h(App)
}).$mount('#app')
