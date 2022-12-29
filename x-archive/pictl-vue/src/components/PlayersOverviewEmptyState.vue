<template>
  <v-row>
    <v-col cols="12" sm="6" md="6">
      <v-card :loading="loading">
        <v-card-title>No Players active</v-card-title>
        <v-card-subtitle>It seems no player does anything!</v-card-subtitle>
        <v-card-text>
          <v-alert
              v-if="wsAudio.state !== 'connected'"
              outlined
              color="deep-orange"
              icon="mdi-lan-disconnect">
            <h3>Websocket</h3>
            {{ wsAudio.state }} to ktor <a v-if="wsAudio.error"
                                           href="https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.1"><b>{{ wsAudio.error }}</b></a>
          </v-alert>
          <v-alert
              v-else
              outlined
              color="light-blue"
              icon="mdi-lan-connect">
            <h3>Websocket</h3>
            {{ wsAudio.state }} to ktor.
          </v-alert>
        </v-card-text>
      </v-card>
    </v-col>
  </v-row>
</template>

<script>
import {mapState} from 'vuex';

export default {
  name: "PlayersOverviewEmptyState",
  computed: {
    ...mapState('players', ['wsAudio']),
    loading() {
      return this.wsAudio.state === 'reconnect'
    }
  }
}
</script>

<style scoped>

</style>