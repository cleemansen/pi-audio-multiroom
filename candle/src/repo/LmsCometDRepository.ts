import { CometD } from "cometd";
import type { Message } from "cometd";
import { useLmsStore } from "@/stores/LmsStore";
import type { Player } from "@/types/Player";

/** Communicates with the cometd-endpoint */
export class LmsCometDRepository {
  private readonly cometD: CometD;
  private connected = false;
  private lmsCometDUrl =
    "https://lms.unividuell.org" /* "http://localhost:9002" */ + "/cometd";

  /** Creates the repository */
  constructor() {
    this.cometD = new CometD();
    // console.debug = console.log;
    this.cometD.unregisterTransport("websocket");

    this.connect();
  }

  /** connects to the cometd-backend */
  connect() {
    this.cometD.addListener("/meta/connect", (message: any) => {
      console.log(`/meta/connect: ` + JSON.stringify(message));
      if (this.connected !== message.successful) {
        this.connected = message.successful;
        console.log(this.connected ? "connected" : "connection failed");
        this.subscribeToRequests();
        this.subscribeToServerStatus();
        this.queryServerStatus();
      }
    });

    console.log("cometD is now initialized");

    this.cometD.configure({
      url: this.lmsCometDUrl,
      useWorkerScheduler: false,
      logLevel: "debug",
    });
    this.cometD.handshake((ack: string) => {
      console.log(`/meta/handshake: ` + JSON.stringify(ack));
    });
  }

  /**
   * connection health check
   * @return {boolean} is-connected
   */
  private checkConnected() {
    if (!this.cometD) {
      console.log("Not connected");
      return false;
    }
    if (!this.connected) {
      console.log("Connection failed");
      return false;
    }
    return true;
  }

  /**
   * Checks weather at least one player is connected or no connected players at all
   * @return {boolean} at least one player is connected
   */
  private checkPlayer() {
    if (!this.checkConnected()) {
      return false;
    }
    if (useLmsStore().players?.length < 1) {
      console.log("no players");
      return false;
    }
    return true;
  }

  /**
   * Subscribe to `/slim/request/*`
   */
  subscribeToRequests() {
    if (this.checkConnected()) {
      this.cometD.subscribe(
        `/${this.cometD.getClientId()}/slim/request/*`,
        (msg) => console.log(`/slim/request/*: ${JSON.stringify(msg)}`),
        (ack) => console.log(`ACK /slim/request/*: ${JSON.stringify(ack)}`)
      );
    }
  }

  /**
   * Subscribe for slim-player-status updates
   */
  subscribeToServerStatus() {
    if (this.checkConnected()) {
      this.cometD.subscribe(
        `/${this.cometD.getClientId()}/slim/serverstatus`,
        (msg) => {
          console.debug(`/slim/serverstatus:`, msg);
          useLmsStore().players = msg.data.players_loop.map((player: any) => {
            return {
              playerId: player.playerid,
              playerName: player.name,
            } as Player;
          });
          this.subscribeToPlayerStatus();
          this.queryPlayerStatus();
        },
        (ack) => console.log(`ACK /slim/serverstatus: ${JSON.stringify(ack)}`)
      );
    }
  }

  /**
   * Query for current slim-server-status
   */
  queryServerStatus() {
    if (this.checkConnected()) {
      this.cometD.publish(
        "/slim/request",
        {
          request: ["", ["serverstatus", 0, 255]],
          response: `/${this.cometD.getClientId()}/slim/serverstatus`,
        },
        (ack) =>
          console.log(
            `ACK /slim/request (serverstatus): ${JSON.stringify(ack)}`
          )
      );
    }
  }

  /**
   * Subscribe for slim-player-status updates
   */
  subscribeToPlayerStatus() {
    if (this.checkConnected()) {
      this.cometD.subscribe(
        `/${this.cometD.getClientId()}/slim/playerstatus/*`,
        (msg: Message) => {
          console.debug(`/slim/playerstatus/*`, msg);
          useLmsStore().updatePlayer(msg);
        },
        (ack) => console.log(`ACK /slim/playerstatus/*: ${JSON.stringify(ack)}`)
      );
    }
  }

  /**
   * Query latest slim-player-status
   */
  queryPlayerStatus() {
    if (this.checkPlayer()) {
      useLmsStore().players.forEach((player: Player) => {
        this.cometD.publish(
          "/slim/request",
          {
            request: [
              `${player.playerId}`,
              ["status", 0, 255, "tags:galKLmNrLT", "subscribe:60"],
            ],
            response: `/${this.cometD.getClientId()}/slim/playerstatus/${
              player.playerId
            }`,
          },
          (ack) =>
            console.log(
              `ACK /slim/request (playerstatus): ${JSON.stringify(ack)}`
            )
        );
      });
    }
  }
}
