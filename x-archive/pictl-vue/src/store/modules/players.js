import Vue from 'vue'
import {Mixin} from '../../mixins/helper'

// initial state
const state = () => ({
    playersMap: {},
    syncNodes: {},
    wsAudio: {
        state: 'disconnected',
        error: null
    }
})

// getters
const getters = {
    nodes: state => {
        return Mixin.methods.objectMap(state.playersMap, player => {
            return Mixin.methods
                .objectFilter(state.syncNodes, ([, candidate]) => candidate.syncController === player.playerId)
        })
    },
}

// actions
const actions = {
    subscribeAudioChange({commit, dispatch}) {
        Vue.prototype.$webSocketsConnect(
            'ctl-audio/ws',
            event => {
                commit('wsState', 'connected')
                dispatch("playerUpdate", event)
            })
    },
    playerUpdate({commit, state}, wsEvent) {
        let playerEvent = JSON.parse(wsEvent.data)
        // console.log(playerEvent)
        if (playerEvent.playerId === playerEvent.syncController || playerEvent.syncController === null) {
            commit('updatePlayer', playerEvent)

            if (state.syncNodes[playerEvent.playerId] !== undefined) {
                // not a node anymore
                commit('removePlayer', playerEvent.playerId)
            }
        }
        if (playerEvent.syncController !== null && playerEvent.syncController !== playerEvent.playerId) {
            // synchronized with somebody else > clean-up
            commit('removePlayer', playerEvent.playerId)
            // but notice UI about this participation
            commit('updateSyncNodes', playerEvent)
        }
    }
}

// mutations
const mutations = {
    updatePlayer(state, playerEvent) {
        Vue.set(state.playersMap, playerEvent.playerId, playerEvent)
    },
    removePlayer(state, playerId) {
        Vue.delete(state.playersMap, playerId)
    },
    updateSyncNodes(state, playerEvent) {
        Vue.set(state.syncNodes, playerEvent.playerId, playerEvent)
    },
    wsState(state, newState) {
        Vue.set(state.wsAudio, 'state', newState)
        if (newState === 'connected') Vue.set(state.wsAudio, 'error', null)
    },
    wsReconnect(state, closeEvent) {
        state.wsAudio = {state: 'reconnecting', error: closeEvent.code}
    }
}

export default {
    namespaced: true,
    state,
    getters,
    actions,
    mutations
}