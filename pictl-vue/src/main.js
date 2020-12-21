import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import vuetify from './plugins/vuetify'
import webSocket from "@/plugins/webSocket";

Vue.config.productionTip = false

Vue.use(webSocket, {
  store,
  host: `ws://${window.location.hostname}:${window.location.port}`
})

new Vue({
  router,
  store,
  vuetify,
  render: h => h(App)
}).$mount('#app')
