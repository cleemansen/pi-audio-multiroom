<template>
  <v-container>
    <v-row>
      <v-col cols="12">
        <v-card>
          <v-card-title>Hello</v-card-title>
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
    //
  }),
  mounted() {
    this.connect()
  },
  methods: {
    connect() {
      this.$webSocketsConnect('/ctl-audio/ws', event => {
        console.log(event)
      })
    },
    send() {
      let d = new Date() + ' hello'
      this.$webSocketsSend(d)
    }
  }
}
</script>

<style scoped>

</style>