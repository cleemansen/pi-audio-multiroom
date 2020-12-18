<template>
  <v-container>
    <v-row>
      <v-col cols="12">
        <v-card v-for="player in players" v-bind:key="player.playerId" class="mb-6">
          <v-img :src="player.artworkUrl">
          </v-img>
          <v-card-text>
            <div>{{ player.playerName }}</div>
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
    playersMap: {}
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
        }
        if (playerEvent.syncMaster !== null && playerEvent.syncMaster !== playerEvent.playerId) {
          // synchronized with somebody else > clean-up
          this.$delete(this.playersMap, playerEvent.playerId)
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