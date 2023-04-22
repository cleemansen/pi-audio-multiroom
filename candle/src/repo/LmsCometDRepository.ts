import { CometD } from "cometd";
import type { Message } from "cometd";
import { useLmsStore } from "../stores/LmsStore";
import type { Player, PlayerServerstatusCometD } from "../types/Player";

/** Communicates with the cometd-endpoint */
export class LmsCometDRepository {
  private readonly cometD: CometD;
  private connected? = false;
  private lmsCometDUrl = import.meta.env.VITE_COMETD_ENDPOINT;

  /** Creates the repository */
  constructor() {
    this.cometD = new CometD();
    this.cometD.unregisterTransport("websocket");

    this.connect();
  }

  /** connects to the cometd-backend */
  connect() {
    this.cometD.addListener("/meta/connect", (message: Message) => {
      console.debug(`/meta/connect: ` + JSON.stringify(message));
      if (this.connected !== message.successful) {
        this.connected = message.successful;
        console.info(this.connected ? "connected" : "connection failed");
        this.subscribeToRequests();
        this.subscribeToSubscriptions();
        this.subscribeToServerStatus();
        this.subscribeToPlayerStatus();
        this.queryServerStatus();
      }
    });

    console.debug("cometD is now initialized");

    this.cometD.configure({
      url: this.lmsCometDUrl,
      useWorkerScheduler: false,
      logLevel: "warn",
    });
    this.cometD.handshake({}, (ack) => {
      console.debug(`/meta/handshake`, ack);
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
        (msg) => console.debug(`/slim/request/*`, msg),
        (ack) => console.debug(`ACK /slim/request/*`, ack)
      );
    }
  }

  /**
   * Subscribe to `/slim/subscribe/*`
   */
  subscribeToSubscriptions() {
    if (this.checkConnected()) {
      this.cometD.subscribe(
        `/slim/subscribe/*`,
        (msg) => console.debug(`/slim/subscribe/*`, msg),
        (ack) => console.debug(`ACK /slim/subscribe/*`, ack)
      );
    }
  }

  /**
   * Subscribe for slim-player-status updates
   */
  subscribeToServerStatus() {
    if (this.checkConnected()) {
      this.cometD.subscribe(
        `/${this.cometD.getClientId()}/candle/serverstatus`,
        (msg) => {
          console.debug(`/candle/serverstatus:`, msg);
          useLmsStore().players = msg.data.players_loop.map(
            (player: PlayerServerstatusCometD) => {
              return {
                playerId: player.playerid,
                playerName: player.name,
              } as Player;
            }
          );
          // this.subscribeToPlayerStatus();
          this.subscribeForPlayerStatusUpdate();
        },
        (ack) => console.debug(`ACK /candle/serverstatus`, ack)
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
        ["serverstatus", 0, 50],
        `/${this.cometD.getClientId()}/candle/serverstatus`
      );
    }
  }

  /**
   * Subscribe for slim-player-status updates
   */
  subscribeToPlayerStatus() {
    if (this.checkConnected()) {
      this.cometD.subscribe(
        `/${this.cometD.getClientId()}/candle/playerstatus/*`,
        (msg: Message) => {
          console.debug(`/candle/playerstatus/*`, msg);
          useLmsStore().updatePlayer(msg);
        },
        (ack) => console.debug(`ACK /candle/playerstatus/*`, ack)
      );
    }
  }

  /**
   * Query latest slim-player-status
   */
  subscribeForPlayerStatusUpdate() {
    if (this.checkPlayer()) {
      useLmsStore()
        .players.concat(useLmsStore().syncNodes)
        .forEach((player: Player) => {
          this.publish(
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
            ["status", 0, 255, "tags:galKLmNrLT", "subscribe:100"],
            "/slim/subscribe",
            `/${this.cometD.getClientId()}/candle/playerstatus/${
              player.playerId
            }`
          );
        });
    }
  }

  /**
   * Do a request to the cometD backend.
   * @param {string} playerId
   * @param {(string|number) []} command
   * @param {string} response
   */
  request(
    playerId: string,
    command: (string | number)[],
    response = "/slim/request"
  ) {
    this.publish(playerId, command, "/slim/request", response);
  }

  /**
   * Publish the command.
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
   * @param {string} channel the channel targeted by this request
   * @param {string} response the response channel
   */
  private publish(
    playerId: string,
    command: (string | number)[],
    channel: string,
    response: string
  ) {
    this.cometD.publish(
      channel,
      {
        response: response,
        request: [playerId, command],
      },
      (ack: Message) => {
        if (ack.successful) {
          console.debug(`request-ack`, ack);
        } else {
          console.warn(`request-ack`, ack);
        }
      }
    );
  }
}
