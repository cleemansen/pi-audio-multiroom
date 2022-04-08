import { CometD } from "cometd";
import type { Message } from "cometd";
import { useLmsStore } from "@/stores/LmsStore";
import type {Player, PlayerServerstatusCometD} from "@/types/Player";

/** Communicates with the cometd-endpoint */
export class LmsCometDRepository {
  private readonly cometD: CometD;
  private connected = false;
  private lmsCometDUrl =
    "https://lms.unividuell.org" /* "http://localhost:9002" */ + "/cometd";

  /** Creates the repository */
  constructor() {
    this.cometD = new CometD();
    this.cometD.unregisterTransport("websocket");

    this.connect();
  }

  /** connects to the cometd-backend */
  connect() {
    this.cometD.addListener("/meta/connect", (message: any) => {
      console.debug(`/meta/connect: ` + JSON.stringify(message));
      if (this.connected !== message.successful) {
        this.connected = message.successful;
        console.info(this.connected ? "connected" : "connection failed");
        this.subscribeToRequests();
        this.subscribeToServerStatus();
        this.queryServerStatus();
      }
    });

    console.debug("cometD is now initialized");

    this.cometD.configure({
      url: this.lmsCometDUrl,
      useWorkerScheduler: false,
      logLevel: "warn",
    });
    this.cometD.handshake((ack: string) => {
      console.debug(`/meta/handshake: ` + JSON.stringify(ack));
    });
  }

  /**
   * connection health check
   * @return {boolean} is-connected
   */
  private checkConnected() {
    if (!this.cometD) {
      console.debug("Not connected");
      return false;
    }
    if (!this.connected) {
      console.warn("Connection failed");
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
      console.debug("no players");
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
        `/slim/request/*`,
        (msg) => console.debug(`/slim/request/*: ${JSON.stringify(msg)}`),
        (ack) => console.debug(`ACK /slim/request/*: ${JSON.stringify(ack)}`)
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
          useLmsStore().players = msg.data.players_loop.map((player: PlayerServerstatusCometD) => {
            return {
              playerId: player.playerid,
              playerName: player.name,
            } as Player;
          });
          this.subscribeToPlayerStatus();
          this.queryPlayerStatus();
        },
        (ack) => console.debug(`ACK /slim/serverstatus: ${JSON.stringify(ack)}`)
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
          console.debug(
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
        (ack) => console.debug(`ACK /slim/playerstatus/*: ${JSON.stringify(ack)}`)
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
            console.debug(
              `ACK /slim/request (playerstatus): ${JSON.stringify(ack)}`
            )
        );
      });
    }
  }

  /**
   * Requests the command.
   * @param {string} playerId targeted player ID
   * @param {string[]} command the command to request to be executed
   */
  request(playerId: string, command: string[]) {
    this.cometD.publish(
        `/slim/request`,
        {
          response: `/${this.cometD.getClientId()}/request`,
          request: [playerId, command],
        },
        (publishAck) => console.warn('request-ack', publishAck)
    );
  }
}
