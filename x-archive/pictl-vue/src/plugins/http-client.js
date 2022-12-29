import axios from "axios";
import Vue from 'vue'

function createInstance() {
    const options = {}

    return axios.create(options)
}

export default {
    install() {
        Vue.prototype.$http = createInstance()
    }
}