<template>
  <div>
    <h1>CometD client status</h1>
    <ul>
      <li>connected: {{this.connected}}</li>
      <li>player-id: {{this.playerId}}</li>
      <li v-if="cometD">client: {{cometD.getStatus()}} {{cometD.getClientId()}}</li>
    </ul>
    <v-btn @click="connect">connect</v-btn>
    <v-btn @click="subscribePlayer">subscribe</v-btn>
    <textarea id="log" readonly cols="80" rows="25" placeholder="Log"></textarea>
  </div>
</template>

<script>
import { CometD } from 'cometd'
import { adapt } from 'cometd-nodejs-client';

export default {
  name: "CometD",
  data: () => ({
    cometD: null,
    connected: false,
    playerId: null
  }),
  mounted() {
    adapt()
    console.debug = console.log
  },
  methods: {
    log(text) {
      let log = document.getElementById("log");
      log.setRangeText(text + "\n");
      log.scrollTop = log.scrollHeight;
      log.setSelectionRange(log.value.length, log.value.length);
    },

    clearLog() {
      let log = document.getElementById("log");
      log.setSelectionRange(0, log.value.length);
      log.setRangeText("");
    },

    checkConnected() {
      if (!this.cometD) {
        this.log("Not connected");
        return false;
      }
      if (!this.connected) {
        this.log("Connection failed");
        return false;
      }
      return true;
    },

    checkPlayer() {
      if (!this.checkConnected()) {
        return false;
      }
      if (!this.playerId) {
        this.log("no player");
        return false;
      }
      return true;
    },

    handleCometDMessage(message) {
      this.log(message.channel);
      if (message.channel.endsWith("/slim/serverstatus")) {
        // Set this.playerId to that of the first player
        if (message.data && message.data.players_loop && message.data.players_loop.length > 0) {
          let playerId = message.data.players_loop[0].playerid;
          let name = message.data.players_loop[0].name;
          if (this.playerId !== playerId) {
            if (this.playerId) {
              this.unsubscribePlayer();
              this.log(`player ${this.playerId} unsubscribed`);
            }
            this.playerId = playerId;
            this.log(`player set to ${this.playerId} (${name})`);
          }
        }
      }
    },

    connect() {
      if (!this.cometD) {
        this.cometD = new CometD();
        console.debug = console.log;
        this.cometD.unregisterTransport('websocket');

        this.cometD.init({
          url: (process.env.NODE_ENV === 'production' ? location.origin : "https://lms.unividuell.org") + "/cometd",
          useWorkerScheduler: false,
          logLevel: "debug"
        });

        this.cometD.addListener("/meta/connect", (message) => {
          if (this.connected !== message.successful) {
            this.connected = message.successful;
            this.log(this.connected ? "connected" : "connection failed");
          }
        });

        this.cometD.addListener("/meta/handshake", (message) => {
          if (message.successful) {
            this.log("handshake ok");
            this.cometD.subscribe(`/${this.cometD.getClientId()}/**`, (message) => this.handleCometDMessage(message));
            this.cometD.subscribe("/slim/subscribe", (msg) => {this.log(msg)},
                {
                  data: {
                    response: `/${this.cometD.getClientId()}/slim/serverstatus`,
                    request: ["", ["serverstatus", 0, 999, "subscribe:0"]]
                  }
                }
            );
          }
          else {
            this.log("handshake failed");
          }
        });
        this.log("cometD is now initialized")
      }
      else {
        this.log("already connected");
      }
    },

    disconnect() {
      if (this.cometD) {
        this.cometD.disconnect(
            (message) => {
              this.log("disconnect:", message.channel);
            });
        this.cometD = undefined;
        this.connected = undefined;
        this.playerId = undefined;
      }
    },

    subscribePlayer() {
      if (this.checkPlayer()) {
        this.cometD.subscribe("/slim/subscribe", () => {},
            {
              data: {
                response: `/${this.cometD.getClientId()}/slim/playerstatus/${this.playerId}`,
                request: [this.playerId, ["status", "-", 1, "subscribe:0"]]
              }
            }
        );
      }
    },

    unsubscribePlayer() {
      if (this.checkPlayer()) {
        this.cometD.subscribe("/slim/subscribe", () => {},
            {
              data: {
                response: `/${this.cometD.getClientId()}/slim/playerstatus/${this.playerId}`,
                request: [this.playerId, ["status", "-", 1, "subscribe:-"]]
              }
            }
        );
      }
    }
  }
}
</script>

<style scoped>

</style>