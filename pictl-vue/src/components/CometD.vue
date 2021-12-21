<template>
  <div>Connected: {{ connected }}</div>
</template>

<script>
import {CometD} from 'cometd'
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
    this.connect()
  },
  methods: {
    log(message) {
      console.log(`app: ${message}`)
    },
    connect() {
      if (this.cometd) {
        // init is equal to configure + handshake
        this.cometd.init({
          url: 'https://lms.unividuell.org/cometd',
          // useWorkerScheduler: false,
          logLevel: "debug"
        })
        this.cometd.addListener("/meta/connect", (message) => {
          this.log(this.connected ? "connected" : "connection failed");
          if (this.connected !== message.successful) {
            this.connected = message.successful;
          }
        });
        this.cometd.addListener("/meta/handshake", (message) => {
          if (message.successful) {
            this.log("handshake ok");
            this.cometd.subscribe(`/${this.cometd.getClientId()}/**`, (message) => this.handleCometDMessage(message));
            this.cometd.subscribe("/slim/subscribe", () => {},
                {
                  data: {
                    response: `/${this.cometd.getClientId()}/slim/serverstatus`,
                    request: ["", ["serverstatus", 0, 999, "subscribe:0"]]
                  }
                }
            );
          } else {
            this.log("handshake failed");
          }
        });
      } else {
        this.log(`already connected.`)
      }
    },
    handleCometDMessage(message) {
      this.log(message.channel);
      if (message.channel.endsWith("/slim/serverstatus")) {
        // Set this.playerId to that of the first player
      //   if (message.data && message.data.players_loop && message.data.players_loop.length > 0) {
      //     let playerId = message.data.players_loop[0].playerid;
      //     let name = message.data.players_loop[0].name;
      //     if (this.playerId !== playerId) {
      //       if (!!this.playerId) {
      //         this.unsubscribePlayer();
      //         this.log(`player ${this.playerId} unsubscribed`);
      //       }
      //       this.playerId = playerId;
      //       this.log(`player set to ${this.playerId} (${name})`);
      //     }
      //   }
      }
    }
  }
}
</script>

<style scoped>

</style>