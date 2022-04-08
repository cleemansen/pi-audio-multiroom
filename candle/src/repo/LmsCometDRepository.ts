import { CometD } from "cometd";
import type { Message } from "cometd";
import { useLmsStore } from "@/stores/LmsStore";
import type {Player, PlayerServerstatusCometD} from "@/types/Player";

/** Communicates with the cometd-endpoint */
export class LmsCometDRepository {
  private readonly cometD: CometD;
  private connected = false;
  private lmsCometDUrl = "https://lms.unividuell.org/cometd";

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
        `/slim/serverstatus`,
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
        (ack) => console.debug(`ACK /slim/serverstatus`, ack)
      );
    }
  }

  /**
   * Query for current slim-server-status
   */
  queryServerStatus() {
    if (this.checkConnected()) {
      this.request(
        "",
        ["serverstatus", 0, 255],
        `/slim/serverstatus`
      );
    }
  }

  /**
   * Subscribe for slim-player-status updates
   */
  subscribeToPlayerStatus() {
    if (this.checkConnected()) {
      this.cometD.subscribe(
        `/slim/playerstatus/*`,
        (msg: Message) => {
          console.debug(`/slim/playerstatus/*`, msg);
          useLmsStore().updatePlayer(msg);
        },
        (ack) => console.debug(`ACK /slim/playerstatus/*`, ack)
      );
    }
  }

  /**
   * Query latest slim-player-status
   */
  queryPlayerStatus() {
    if (this.checkPlayer()) {
      useLmsStore().players.forEach((player: Player) => {
        this.request(
          player.playerId,
          // g: Genre
          // a: Artist
          // l: Album
          // K: artwork_url
          // L:  info_link
          // m: bpm
          // N: Title of the internet radio station.
          // T: samplerate Song sample rate (in KHz)
          // r: bitrate
          // u: Song file url.
          ["status", 0, 255, "tags:galKLmNrLT", "subscribe:60"],
          `/slim/playerstatus/${player.playerId}`
        );
      });
    }
  }

  /**
   * Requests the command.
   * Backend: https://github.com/Logitech/slimserver/blob/public/8.3/Slim/Web/Cometd.pm#L512
   * A valid /slim/request message looks like this:
   * ```
   * {
   *   channel  => '/slim/request',
   *   id       => <unique id>, (optional)
   *   data     => {
   *     response => '/slim/<clientId>/request',
   *     request  => [ '', [ 'menu', 0, 100, ],
   *     priority => <value>, # optional priority value, is passed-through with the response
   *   }
   * }
   * ```
   * @param {string} playerId targeted player ID
   * @param {(string | number)[]} command the command to request to be executed
   * @param {string} response the response channel
   */
  request(
    playerId: string,
    command: (string | number)[],
    response = `/${this.cometD.getClientId()}/request`
  ) {
    this.cometD.publish(
      `/slim/request`,
      {
        response: response,
        request: [playerId, command],
      },
      (publishAck) => console.debug(`request-ack`, publishAck)
    );
  }
}
