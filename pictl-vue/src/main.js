import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import vuetify from './plugins/vuetify'
import webSocket from "@/plugins/webSocket";

Vue.config.productionTip = false

Vue.use(webSocket, {
  store,
  host: 'ws://192.168.0.156:8080'
})

new Vue({
  router,
  store,
  vuetify,
  render: h => h(App)
}).$mount('#app')
