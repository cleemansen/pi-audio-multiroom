<template>
  <div>
    <div class="row">
      <h1>CometD client status</h1>
      <ul>
        <li>connected: {{ this.connected }}</li>
        <li>players: {{ JSON.stringify(this.players) }}</li>
        <li>current-title: {{ this.currentTitle }}</li>
        <li v-if="cometD">client-id: {{ cometD.getStatus() }} {{ cometD.getClientId() }}</li>
      </ul>
    </div>
    <div class="row">
      <v-btn @click="connect">connect</v-btn>
      <v-btn @click="disconnect">disconnect</v-btn>
      <v-btn @click="clearLog">clear log</v-btn>
    </div>
    <div class="row">
      <v-btn @click="subscribeToRequests">sub requests</v-btn>
    </div>
    <div class="row">
      <v-btn @click="subscribeToServerStatus">sub serverstatus</v-btn>
      <v-btn @click="queryServerStatus">query serverstatus</v-btn>
    </div>
    <div class="row">
      <v-btn @click="subscribeToPlayerStatus">sub playerstatus</v-btn>
      <v-btn @click="queryPlayerStatus">query playerstatus</v-btn>
    </div>
    <div class="row">
      <v-btn @click="subscribeToDisplayStatus">sub displaystatus</v-btn>
      <v-btn @click="queryDisplayStatus">query displaystatus</v-btn>
    </div>
    <div class="row">
      <v-btn @click="subscribeToMenuStatus">sub menustatus</v-btn>
      <v-btn @click="queryMenuStatus">query menustatus</v-btn>
    </div>
    <div class="row">
      <textarea id="log" readonly cols="80" rows="25" placeholder="Log" style="font-family: monospace; font-size: 9pt"></textarea>
    </div>
  </div>
</template>

<script>
import {CometD} from 'cometd'
import {adapt} from 'cometd-nodejs-client';

export default {
  name: "CometD",
  data: () => ({
    cometD: null,
    connected: false,
    players: [],
    currentTitle: undefined,
    logLine: 0
  }),
  mounted() {
    adapt()
    console.debug = console.log
  },
  methods: {
    log(text) {
      let log = document.getElementById("log");
      let line = `${this.logLine++}: `
      if (text && typeof text === 'object' && text.constructor === Object) {
        line += JSON.stringify(text);
      } else {
        line += text
      }
      log.setRangeText(line + "\n");
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
      if (this.players?.length < 1) {
        this.log("no players");
        return false;
      }
      return true;
    },

    connect() {
      if (!this.cometD) {
        this.cometD = new CometD();
        console.debug = console.log;
        this.cometD.unregisterTransport('websocket');

        this.cometD.addListener("/meta/connect", (message) => {
          this.log(`/meta/connect: ` + JSON.stringify(message))
          if (this.connected !== message.successful) {
            this.connected = message.successful;
            this.log(this.connected ? "connected" : "connection failed");
          }
        });

        this.cometD.addListener("/meta/handshake", (message) => {
          this.log(`/meta/handshake: ` + JSON.stringify(message))
        });
        this.log("cometD is now initialized")
      } else {
        this.log("already connected");
      }

      this.cometD.init({
        url: (process.env.NODE_ENV === 'production' ? location.origin : "https://lms.unividuell.org") + "/cometd",
        useWorkerScheduler: false,
        logLevel: "debug"
      });
    },

    disconnect() {
      if (this.cometD) {
        this.cometD.disconnect(
            (message) => {
              this.log("disconnect: ", message.channel);
            });
        this.cometD = undefined;
        this.connected = undefined;
        this.players = [];
      }
    },

    subscribeToRequests() {
      if (this.checkConnected()) {
        this.cometD.subscribe(
            `/${this.cometD.getClientId()}/slim/request/*`,
            (msg) => this.log(`/slim/request/*: ${JSON.stringify(msg)}`),
            (ack) => this.log(`ACK /slim/request/*: ${JSON.stringify(ack)}`)
        )
      }
    },

    subscribeToServerStatus() {
      if (this.checkConnected()) {
        this.cometD.subscribe(
            `/${this.cometD.getClientId()}/slim/serverstatus`,
            (msg) => {
              this.log(`/slim/serverstatus: ${JSON.stringify(msg)}`)
              this.players = msg.data.players_loop.map((player) => {
                    return {
                      id: player.playerid,
                      name: player.name
                    }
              })
            },
            (ack) => this.log(`ACK /slim/serverstatus: ${JSON.stringify(ack)}`)
        )
      }
    },
    queryServerStatus() {
      if (this.checkConnected()) {
        this.cometD.publish(
            "/slim/request",
            {
              request: ["", ["serverstatus", 0, 255]],
              response: `/${this.cometD.getClientId()}/slim/serverstatus`
            },
            (ack) => this.log(`ACK /slim/request (serverstatus): ${JSON.stringify(ack)}`)
        )
      }
    },

    subscribeToPlayerStatus() {
      if (this.checkConnected()) {
        this.cometD.subscribe(
            `/${this.cometD.getClientId()}/slim/playerstatus/*`,
            (msg) => {
              this.log(`/slim/playerstatus/*: ${JSON.stringify(msg)}`)
              this.currentTitle = msg.data.remoteMeta.title
            },
            (ack) => this.log(`ACK /slim/playerstatus/*: ${JSON.stringify(ack)}`)
        )
      }
    },
    queryPlayerStatus() {
      if (this.checkPlayer()) {
        this.players.forEach((player) => {
          this.cometD.publish(
              "/slim/request",
              {
                request: [`${player.id}`, ["status", 0, 255, "tags:galKLmNrLT", "subscribe:60"]],
                response: `/${this.cometD.getClientId()}/slim/playerstatus/${player.id}`
              },
              (ack) => this.log(`ACK /slim/request (playerstatus): ${JSON.stringify(ack)}`)
          )
        })
      }
    },

    subscribeToDisplayStatus() {
      if (this.checkConnected()) {
        this.cometD.subscribe(
            `/${this.cometD.getClientId()}/slim/displaystatus/*`,
            (msg) => this.log(`/slim/displaystatus/*: ${JSON.stringify(msg)}`),
            (ack) => this.log(`ACK /slim/displaystatus/*: ${JSON.stringify(ack)}`)
        )
      }
    },
    queryDisplayStatus() {
      if (this.checkPlayer()) {
        this.players.forEach((player) => {
          this.cometD.publish(
              "/slim/request",
              {
                request: [`${player.id}`, ["status", "-", 1, "useContextMenu:1", "subscribe:0", "menu:menu"]],
                response: `/${this.cometD.getClientId()}/slim/displaystatus/${player.id}`
              },
              (ack) => this.log(`ACK /slim/request (displaystatus): ${JSON.stringify(ack)}`)
          )
        })
      }
    },

    subscribeToMenuStatus() {
      if (this.checkConnected()) {
        this.cometD.subscribe(
            `/${this.cometD.getClientId()}/slim/menustatus/*`,
            (msg) => this.log(`/slim/menustatus/*: ${JSON.stringify(msg)}`),
            (ack) => this.log(`ACK /slim/menustatus/*: ${JSON.stringify(ack)}`)
        )
      }
    },
    queryMenuStatus() {
      if (this.checkPlayer()) {
        this.players.forEach((player) => {
          this.cometD.publish(
              "/slim/request",
              {
                request: [`${player.id}`, ["menustatus"]],
                response: `/${this.cometD.getClientId()}/slim/menustatus/${player.id}`
              },
              (ack) => this.log(`ACK /slim/request (menustatus): ${JSON.stringify(ack)}`)
          )
        })
      }
    },

  }
}
</script>

<style scoped>

</style>