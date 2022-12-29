import Vue from 'vue'
import VueRouter from 'vue-router'
import CometD from "@/components/CometD";
import PlayersOverview from "@/components/PlayersOverview";

Vue.use(VueRouter)

const routes = [
  {
    path: '/cometd',
    name: 'cometd-playground',
    component: CometD
  },
  {
    path: '/',
    name: 'App',
    component: PlayersOverview
  },
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

export default router
