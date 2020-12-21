<template>
  <v-container>
    <v-row>
      <v-col cols="12" sm="6" md="6" v-for="player in players" v-bind:key="player.playerId">
        <v-card class="mb-6">
          <v-img :src="player.artworkUrl">
          </v-img>
          <v-card-actions class="mt-4">
            <v-spacer></v-spacer>
            <v-btn fab large @click="togglePlayPause(player)" class="btn-fix">
              <v-icon>{{ playPausePlayerState(player) }}</v-icon>
            </v-btn>
            <v-btn fab large>
              <v-icon>mdi-stop</v-icon>
            </v-btn>
            <v-spacer></v-spacer>
          </v-card-actions>
          <v-card-text>
            <div>{{ playerName(player) }}</div>
            <p class="display-1 text--primary">{{ currentSong(player) }}</p>
            <p class="display-2 text--primary">{{ player.remoteTitle }}</p>
          </v-card-text>
          <v-card-actions class="mt-4">
            <v-spacer></v-spacer>
            <v-btn fab large>
              <v-icon>mdi-power</v-icon>
            </v-btn>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script>
export default {
  name: "PlayersOverview",
  data: () => ({
    playersMap: {},
    syncNodes: []
  }),
  mounted() {
    this.connect()
  },
  methods: {
    connect() {
      this.$webSocketsConnect('/ctl-audio/ws', event => {
        let playerEvent = JSON.parse(event.data)
        console.log(playerEvent)
        if (playerEvent.playerId === playerEvent.syncController || playerEvent.syncController === null) {
          this.$set(this.playersMap, playerEvent.playerId, playerEvent)
          if (this.syncNodes.includes(playerEvent.playerName)) {
            // not a node anymore
            this.syncNodes.splice(this.syncNodes.indexOf(playerEvent.playerName, 1))
          }
        }
        if (playerEvent.syncController !== null && playerEvent.syncController !== playerEvent.playerId) {
          // synchronized with somebody else > clean-up
          this.$delete(this.playersMap, playerEvent.playerId)
          // but notice UI about this participation
          if (!this.syncNodes.includes(playerEvent.playerName)) {
            this.syncNodes.push(playerEvent.playerName)
          }
        }
      })
    },
    togglePlayPause(player) {
      let d = {
        type: "cmd",
        cmd: "TOGGLE_PLAY_PAUSE",
        playerId: player.playerId
      }
      this.$webSocketsSend(d)
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
      if (this.syncNodes.length >= 1) {
        this.syncNodes.forEach(node => buffer.push(node))
      }
      return buffer.join(' & ')
    },
    playPausePlayerState(player) {
      if (player.mode) {
        if (player.mode === 'play') {
          return 'mdi-pause'
        } else if (player.mode === 'pause' || player.mode === 'stop') {
          return 'mdi-play'
        }
      }
      return 'mdi-heart-broken'
    }
  },
  computed: {
    players() {
      return this.playersMap
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