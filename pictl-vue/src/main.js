import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import httpclient from '@/plugins/http-client'
import vuetify from './plugins/vuetify'
import webSocket from "@/plugins/webSocket"
import '@/mixins/helper'

Vue.config.productionTip = false

let endpoint = `${window.location.hostname}:${window.location.port}`
Vue.use(webSocket, {
  store,
  host: `ws://${endpoint}`
})
Vue.use(httpclient)

new Vue({
  router,
  store,
  vuetify,
  render: h => h(App)
}).$mount('#app')
