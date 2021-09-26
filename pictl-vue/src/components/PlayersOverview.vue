<template>
  <v-container>
    <PlayersOverviewEmptyState v-if="this.emptyState"/>
    <v-row v-else>
      <v-col cols="12" sm="6" md="6" v-for="player in playersMap" v-bind:key="player.playerId">
        <v-card class="mb-6">
          <v-system-bar>
            <v-icon>mdi-cast-audio</v-icon>
            <span>pictl for {{ playerName(player) }}</span>
          </v-system-bar>
          <v-toolbar>
            <v-app-bar-nav-icon></v-app-bar-nav-icon>
            <v-spacer/>

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

            <v-spacer/>
            <v-btn icon @click="shutdown(player)">
              <v-icon>mdi-power</v-icon>
            </v-btn>
          </v-toolbar>
          <v-row>
            <v-col cols="12" class="pb-4">
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
            <CurrentTitle :artist="player.artist" :title="player.title"/>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script>
import PlayerVolume from "@/components/PlayerVolume";
import CurrentTitle from "@/components/CurrentTitle";
import PlayersOverviewEmptyState from "@/components/PlayersOverviewEmptyState";
import {mapGetters, mapState} from 'vuex'

export default {
  name: "PlayersOverview",
  components: {PlayersOverviewEmptyState, CurrentTitle, PlayerVolume},
  data: () => ({
    desiredState: {}
  }),
  mounted() {
    this.connect()
  },
  methods: {
    connect() {
      this.$store.dispatch('players/subscribeAudioChange')
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
    ...mapState('players', ['playersMap', 'syncNodes']),
    ...mapGetters('players', ['nodes']),
    emptyState() {
      // kudos: https://stackoverflow.com/a/32108184/810944
      return this.playersMap
          && Object.keys(this.playersMap).length === 0
          && Object.getPrototypeOf(this.playersMap) === Object.prototype
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