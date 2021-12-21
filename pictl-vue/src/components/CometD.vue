<template>
  <div>Connected: {{ connected }}</div>
</template>

<script>
import {CometD} from 'cometd';
import {adapt} from 'cometd-nodejs-client';

export default {
  name: "CometD",
  data: () => ({
    cometd: null,
    connected: false
  }),
  mounted() {
    adapt()
    this.cometd = new CometD()
    console.debug = console.log
    this.cometd.configure({
      url: 'https://lms.unividuell.org/cometd',
      logLevel: "debug"
    })
    this.cometd.init((handshakeReply) => {
      this.connected = handshakeReply.successful
    })
  }
}
</script>

<style scoped>

</style>