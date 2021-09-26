import Vue from 'vue'
import Vuex from 'vuex'
import players from "@/store/modules/players";

Vue.use(Vuex)

const debug = process.env.NODE_ENV !== 'production'

export default new Vuex.Store({
  modules: {
    players
  },
  strict: debug
})
