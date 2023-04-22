import { defineStore } from "pinia";
import { LmsCometDRepository } from "../repo/LmsCometDRepository";
import type { Player, PlayerCometD } from "../types/Player";
import type { Message } from "cometd";
import { computed, ref } from "vue";

export const useLmsStore = defineStore("lms", () => {
  const players = ref<Player[]>([]);
  const syncNodes = ref<Player[]>([]);
  const cometD = new LmsCometDRepository();

  const currentTitle = computed((): string => {
    if (players.value.length < 1) {
      return "n/a";
    }
    return `${players.value[0].artist}: ${players.value[0].title} [${players.value[0].remoteTitle}]`;
  });

  /**
   * Applies the message to the player state.
   * @param {Message} msg: the cometD message
   */
  function updatePlayer(msg: Message) {
    const playerId = msg.channel.substring(msg.channel.lastIndexOf("/") + 1);
    const playerEvent = mapPlayerEvent(playerId, msg.data as PlayerCometD);
    // this.$patch((state) => {
    if (
      playerEvent.playerId === playerEvent.syncController ||
      playerEvent.syncController === undefined
    ) {
      const playerIdx = players.value.findIndex(
        (player: Player) => player.playerId === playerId
      );
      if (playerIdx > -1) {
        players.value[playerIdx] = playerEvent;
      } else {
        players.value.push(playerEvent);
      }

      if (
        syncNodes.value.some(
          (syncPlayer) => syncPlayer.playerId === playerEvent.playerId
        )
      ) {
        // not a node anymore
        removePlayer(playerEvent.playerId);
      }
    }
    if (
      playerEvent.syncController !== undefined &&
      playerEvent.syncController !== playerEvent.playerId
    ) {
      // synchronized with somebody else => clean-up
      removePlayer(playerEvent.playerId);
      // but notice UI about this participation
      updateSyncNodes(playerEvent);
    }
    // });
  }

  /**
   * Removes the player with the given ID.
   * @param {string} playerId player ID to remove
   */
  function removePlayer(playerId: string) {
    const playerIdx = players.value.findIndex(
      (player: Player) => player.playerId === playerId
    );
    if (playerIdx > -1) {
      players.value.splice(playerIdx, 1);
    }
  }

  /**
   * Updates a node that is synchronised
   * @param {Player} playerEvent the event of the player
   */
  function updateSyncNodes(playerEvent: Player) {
    const playerIdx = syncNodes.value.findIndex(
      (player: Player) => player.playerId === playerEvent.playerId
    );
    if (playerIdx > -1) {
      syncNodes.value[playerIdx] = playerEvent;
    } else {
      syncNodes.value.push(playerEvent);
    }
  }

  /**
   * Toggles the play/pause-state of the given player.
   * @param {string} playerId player ID
   */
  function togglePlayPause(playerId: string) {
    cometD.request(playerId, ["pause"]);
    // this.cometD.queryPlayerStatus();
  }

  /**
   * Set the volume of the player to the given value.
   * @param {string} playerId
   * @param {number} desiredVolume desired volume
   */
  function volume(playerId: string, desiredVolume: number) {
    cometD.request(playerId, ["mixer", "volume", `${desiredVolume}`]);
    // this.cometD.queryPlayerStatus();
  }

  /**
   * Steps down the volume by the given step.
   * @param {string} playerId
   * @param {number} step: the step to lower the volume
   */
  function volumeStepDown(playerId: string, step = 4) {
    cometD.request(playerId, ["mixer", "volume", `-${step}`]);
    // this.cometD.queryPlayerStatus();
  }
  /**
   * Steps up the volume by the given step.
   * @param {string} playerId
   * @param {number} step: the step to upper the volume
   */
  function volumeStepUp(playerId: string, step = 4) {
    cometD.request(playerId, ["mixer", "volume", `+${step}`]);
    // this.cometD.queryPlayerStatus();
  }

  /**
   * Returns the name of the requested player.
   * @param {string} playerId
   * @return {string} the name of the player
   */
  function playerName(playerId: string): string {
    const buffer = [] as string[];
    buffer.push(
      players.value.find((player: Player) => player.playerId === playerId)
        ?.playerName ?? "n/a"
    );
    syncNodes.value.forEach((syncNode) =>
      buffer.push(syncNode.playerName ?? "n/a")
    );
    return buffer.join(" & ") as string;
  }

  return {
    players,
    syncNodes,
    updatePlayer,
    togglePlayPause,
    volume,
    volumeStepUp,
    volumeStepDown,
    currentTitle,
    playerName,
  };
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
