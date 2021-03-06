<template>
  <v-container>
    <v-row>
      <v-col cols="12" sm="6" md="6" v-for="player in players" v-bind:key="player.playerId">
        <v-card class="mb-6">
          <v-system-bar>
            <v-icon>mdi-cast-audio</v-icon>
            <span>pictl</span>
          </v-system-bar>
          <v-toolbar>
            <v-app-bar-nav-icon></v-app-bar-nav-icon>
            <v-toolbar-title>{{ playerName(player) }}</v-toolbar-title>

            <v-spacer></v-spacer>
            <v-btn icon x-large @click="volumeStepDown(player)">
              <v-icon>mdi-volume-medium</v-icon>
            </v-btn>
            <v-btn
                icon x-large
                @click="togglePlayPause(player)"
                :loading="!reachedDesiredMode[player.playerId]">
              <v-icon>{{ playPauseIcon[player.playerId] }}</v-icon>
            </v-btn>
            <v-btn icon x-large @click="volumeStepUp(player)">
              <v-icon>mdi-volume-high</v-icon>
            </v-btn>
            <v-btn icon @click="shutdown(player)">
              <v-icon>mdi-power</v-icon>
            </v-btn>
          </v-toolbar>
          <v-row>
            <v-col cols="12" class="pt-1 pb-1">
              <PlayerVolume
                  :key="player.playerId"
                  :player-id="player.playerId"
                  :player-name="player.playerName"
                  :mixer-volume="player.mixerVolume"
                  v-on:desired-volume="volumeChange"/>
              <PlayerVolume
                  v-for="node in nodes[player.playerId]" v-bind:key="node.playerId"
                  :player-id="node.playerId"
                  :player-name="node.playerName"
                  :mixer-volume="node.mixerVolume"
                  v-on:desired-volume="volumeChange"/>
            </v-col>
          </v-row>
          <v-img :src="player.artworkUrl">
          </v-img>
          <v-card-text>
            <p class="display-1 text--primary">{{ currentSong(player) }}</p>
            <p class="display-2 text--primary">{{ player.remoteTitle }}</p>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script>
import PlayerVolume from "@/components/PlayerVolume";

export default {
  name: "PlayersOverview",
  components: {PlayerVolume},
  data: () => ({
    playersMap: {},
    syncNodes: {},
    desiredState: {}
  }),
  mounted() {
    this.connect()
  },
  methods: {
    connect() {
      this.$webSocketsConnect('/ctl-audio/ws', event => {
        let playerEvent = JSON.parse(event.data)
        // console.log(playerEvent)
        if (playerEvent.playerId === playerEvent.syncController || playerEvent.syncController === null) {
          this.$set(this.playersMap, playerEvent.playerId, playerEvent)
          if (this.syncNodes[playerEvent.playerId] !== undefined) {
            // not a node anymore
            this.$delete(this.syncNodes, playerEvent.playerId)
          }
        }
        if (playerEvent.syncController !== null && playerEvent.syncController !== playerEvent.playerId) {
          // synchronized with somebody else > clean-up
          this.$delete(this.playersMap, playerEvent.playerId)
          // but notice UI about this participation
          this.$set(this.syncNodes, playerEvent.playerId, playerEvent)
        }
      })
    },
    togglePlayPause(player) {
      let cmdRequest = {
        type: "cmd",
        cmd: "TOGGLE_PLAY_PAUSE",
        playerId: player.playerId
      }
      this.$set(this.desiredState, player.playerId, {mode: this.oppositePlayPauseState(player.mode)})
      this.$webSocketsSend(cmdRequest)
    },
    oppositePlayPauseState(currentState) {
      if (currentState === 'play') return 'pause'
      if (currentState === 'pause') return 'play'
    },
    volumeStepUp(player) {
      let cmdRequest = {
        type: "cmd",
        cmd: "VOLUME_STEP_UP",
        playerId: player.playerId
      }
      this.$webSocketsSend(cmdRequest)
    },
    volumeStepDown(player) {
      let cmdRequest = {
        type: "cmd",
        cmd: "VOLUME_STEP_DOWN",
        playerId: player.playerId
      }
      this.$webSocketsSend(cmdRequest)
    },
    volumeChange(playerId, desiredVolume) {
      let cmdRequest = {
        type: "cmd",
        cmd: "VOLUME_CHANGE",
        playerId: playerId,
        desiredVolume: desiredVolume
      }
      this.$webSocketsSend(cmdRequest)
    },
    currentSong(player) {
      let currentSong = ""
      if (player.artist) {
        currentSong += player.artist
      }
      if (player.title) {
        if (currentSong !== "") {
          currentSong += " — "
        }
        currentSong += player.title
      }
      return currentSong
    },
    playerName(player) {
      let buffer = [player.playerName]
      let nodesOfPlayer = this.nodes[player.playerId]
      if (nodesOfPlayer) {
        let nodeNames = this.objectMap(nodesOfPlayer, node => node.playerName)
        Array.prototype.push.apply(buffer, Object.values(nodeNames))
      }
      return buffer.join(' & ')
    },
    shutdown(player) {
      let playerIps = [player.ipAddress]
      let nodeIps = this.objectMap(this.nodes[player.playerId], node => node.ipAddress)
      if (nodeIps) {
        Array.prototype.push.apply(playerIps, Object.values(nodeIps))
      }
      this.$http
          .post("/ctl-hardware/shutdown", {ips: playerIps})
          .then(response => console.log(`shutdown result for [${nodeIps}]: ${response}`))
          .catch(err => console.log(`shutdown result for [${nodeIps}]: ${err}`))
    }
  },
  computed: {
    players() {
      return this.playersMap
    },
    nodes() {
      return this.objectMap(this.playersMap, player => {
        return this
            .objectFilter(this.syncNodes, ([, candidate]) => candidate.syncController === player.playerId)
      })
    },
    playPauseIcon() {
      return this.objectMap(this.playersMap, player => {
        if (player.mode) {
          if (player.mode === 'play') {
            return 'mdi-pause'
          } else if (player.mode === 'pause' || player.mode === 'stop') {
            return 'mdi-play'
          }
        }
        return 'mdi-heart-broken'
      })
    },
    reachedDesiredMode() {
      return this.objectMap(this.playersMap, player => {
        if (this.desiredState[player.playerId]?.mode === undefined) {
          // we are not waiting for a desired mode
          return true
        }
        if (this.desiredState[player.playerId].mode === player.mode) {
          // reached our goal
          return true
        } else {
          // waiting
          return false
        }
      })
    }
  }
}
</script>

<style scoped>
/* FAB fix highlight state after clicking START */
/* kudos: https://github.com/vuetifyjs/vuetify/issues/3125 */
.btn-fix:focus::before {
  opacity: 0 !important;
}

.btn-fix:hover::before {
  opacity: 0.08 !important;
}

/* END */
</style>