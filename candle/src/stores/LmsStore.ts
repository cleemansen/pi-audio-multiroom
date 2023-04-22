import { defineStore } from "pinia";
import { LmsCometDRepository } from "../repo/LmsCometDRepository";
import type { Player, PlayerCometD } from "../types/Player";
import type { Message } from "cometd";

export const useLmsStore = defineStore("lms", {
  state: () => ({
    players: [] as Player[],
    syncNodes: [] as Player[],
    cometD: new LmsCometDRepository(),
  }),
  actions: {
    updatePlayer(msg: Message) {
      const playerId = msg.channel.substring(msg.channel.lastIndexOf("/") + 1);
      const playerEvent = mapPlayerEvent(playerId, msg.data as PlayerCometD);
      this.$patch((state) => {
        if (
          playerEvent.playerId === playerEvent.syncController ||
          playerEvent.syncController === undefined
        ) {
          const playerIdx = this.players.findIndex(
            (player: Player) => player.playerId === playerId
          );
          if (playerIdx > -1) {
            state.players[playerIdx] = playerEvent;
          } else {
            state.players.push(playerEvent);
          }

          if (
            state.syncNodes.some(
              (syncPlayer) => syncPlayer.playerId === playerEvent.playerId
            )
          ) {
            // not a node anymore
            this.removePlayer(playerEvent.playerId);
          }
        }
        if (
          playerEvent.syncController !== undefined &&
          playerEvent.syncController !== playerEvent.playerId
        ) {
          // synchronized with somebody else => clean-up
          this.removePlayer(playerEvent.playerId);
          // but notice UI about this participation
          this.updateSyncNodes(playerEvent);
        }
      });
    },
    removePlayer(playerId: string) {
      const playerIdx = this.players.findIndex(
        (player: Player) => player.playerId === playerId
      );
      if (playerIdx > -1) {
        this.players.splice(playerIdx, 1);
      }
    },
    updateSyncNodes(playerEvent: Player) {
      const playerIdx = this.syncNodes.findIndex(
        (player: Player) => player.playerId === playerEvent.playerId
      );
      if (playerIdx > -1) {
        this.syncNodes[playerIdx] = playerEvent;
      } else {
        this.syncNodes.push(playerEvent);
      }
    },
    togglePlayPause(playerId: string) {
      this.cometD.request(playerId, ["pause"]);
      // this.cometD.queryPlayerStatus();
    },
    volume(playerId: string, desiredVolume: number) {
      this.cometD.request(playerId, ["mixer", "volume", `${desiredVolume}`]);
      // this.cometD.queryPlayerStatus();
    },
    volumeStepDown(playerId: string, step = 4) {
      this.cometD.request(playerId, ["mixer", "volume", `-${step}`]);
      // this.cometD.queryPlayerStatus();
    },
    volumeStepUp(playerId: string, step = 4) {
      this.cometD.request(playerId, ["mixer", "volume", `+${step}`]);
      // this.cometD.queryPlayerStatus();
    },
  },
  getters: {
    currentTitle(): string {
      if (this.players.length < 1) {
        return "n/a";
      }
      return `${this.players[0].artist}: ${this.players[0].title} [${this.players[0].remoteTitle}]`;
    },
    playerName: (state) => {
      return (playerId: string) => {
        const buffer = [] as string[];
        buffer.push(
          state.players?.find((player: Player) => player.playerId === playerId)
            ?.playerName ?? "n/a"
        );
        state.syncNodes.forEach((syncNode) =>
          buffer.push(syncNode.playerName ?? "n/a")
        );
        return buffer.join(" & ") as string;
      };
    },
  },
});

/**
 * Parse the URL of an artwork.
 * @param {string} lmsArtworkUrl the URL of the artwork, returned by slim
 * @return {string} resolvable artwork-url
 */
function parseArtworkUrl(
  lmsArtworkUrl: string | null | undefined
): string | null {
  if (!lmsArtworkUrl) return null;

  if (
    lmsArtworkUrl.startsWith("/imageproxy/") ||
    lmsArtworkUrl.startsWith("/plugins/") ||
    lmsArtworkUrl.startsWith("html/")
  ) {
    return (
      `http://thin.unividuell.org:9000` +
      (lmsArtworkUrl.startsWith("/") ? "" : "/") +
      lmsArtworkUrl
    );
  } else {
    return lmsArtworkUrl;
  }
}

/**
 * Maps a slim player event to our domain model
 * @param {string} playerId the ID of the player
 * @param {PlayerCometD} playerEvent the event from slim via cometd
 * @return {Player} the mapped event
 */
function mapPlayerEvent(playerId: string, playerEvent: PlayerCometD): Player {
  return {
    playerId: playerId,
    playerName: playerEvent.player_name,
    title: playerEvent.remoteMeta?.title,
    artist: playerEvent.remoteMeta?.artist,
    remoteTitle: playerEvent.remoteMeta?.remote_title,
    artworkUrl: parseArtworkUrl(playerEvent.remoteMeta?.artwork_url),
    mode: playerEvent.mode,
    mixerVolume: playerEvent["mixer volume"],
    connected: playerEvent.player_connected === 1,
    ipAddress: playerEvent.player_ip,
    syncController: playerEvent.sync_master,
    syncNodes: playerEvent.sync_slaves?.split(",") ?? [],
  } as Player;
}
