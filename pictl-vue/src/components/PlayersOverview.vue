<template>
  <v-container>
    <v-row>
      <v-col cols="12" sm="6" md="6" v-for="player in players" v-bind:key="player.playerId">
        <v-card class="mb-6">
          <v-img :src="player.artworkUrl">
          </v-img>
          <v-card-text>
            <div>{{ playerName(player) }}</div>
            <p class="display-1 text--primary">{{ currentSong(player) }}</p>
            <p class="display-2 text--primary">{{ player.remoteTitle }}</p>
          </v-card-text>
          <v-card-actions>
            <v-btn @click="send">send</v-btn>
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
    syncSlaves: []
  }),
  mounted() {
    this.connect()
  },
  methods: {
    connect() {
      this.$webSocketsConnect('/ctl-audio/ws', event => {
        let playerEvent = JSON.parse(event.data)
        console.log(playerEvent)
        if (playerEvent.playerId === playerEvent.syncMaster || playerEvent.syncMaster === null) {
          this.$set(this.playersMap, playerEvent.playerId, playerEvent)
          if (this.syncSlaves.includes(playerEvent.playerName)) {
            // not a slave anymore
            this.syncSlaves.splice(this.syncSlaves.indexOf(playerEvent.playerName, 1))
          }
        }
        if (playerEvent.syncMaster !== null && playerEvent.syncMaster !== playerEvent.playerId) {
          // synchronized with somebody else > clean-up
          this.$delete(this.playersMap, playerEvent.playerId)
          // but notice UI about this participation
          if (!this.syncSlaves.includes(playerEvent.playerName)) {
            this.syncSlaves.push(playerEvent.playerName)
          }
        }
      })
    },
    send() {
      let d = new Date() + ' hello'
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
      if (this.syncSlaves.length >= 1) {
        buffer.push(this.syncSlaves)
      }
      return buffer.join(' & ')
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

</style>