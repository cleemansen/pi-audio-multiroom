import {defineStore} from "pinia";
import {LmsCometDRepository} from "../repo/LmsCometDRepository";
import {Player, PlayerCometD} from "../types/Player";
import {Message} from "cometd";

export const useLmsStore = defineStore('playerStatus', {
    state: () => ({
        players: [] as Player[],
        syncNodes: [] as Player[],
        cometD: new LmsCometDRepository(),
    }),
    actions: {
        updatePlayer(msg: Message) {
            let playerId = msg.channel.substring(msg.channel.lastIndexOf('/') + 1)
            let playerEvent = mapPlayerEvent(playerId, (msg.data as PlayerCometD))
            this.$patch((state) => {
                if (playerEvent.playerId === playerEvent.syncController || playerEvent.syncController === undefined) {
                    let playerIdx = this.players
                        .findIndex((player: Player) => player.playerId === playerId)
                    if (playerIdx > -1) {
                        state.players[playerIdx] = playerEvent
                    } else {
                        state.players.push(playerEvent)
                    }

                    if (state.syncNodes.some((syncPlayer) => syncPlayer.playerId === playerEvent.playerId)) {
                        // not a node anymore
                        this.removePlayer(playerEvent.playerId)
                    }
                }
                if (playerEvent.syncController !== undefined && playerEvent.syncController !== playerEvent.playerId) {
                    // synchronized with somebody else => clean-up
                    this.removePlayer(playerEvent.playerId)
                    // but notice UI about this participation
                    this.updateSyncNodes(playerEvent)
                }
            })
        },
        removePlayer(playerId: string) {
            let playerIdx = this.players
                .findIndex((player: Player) => player.playerId === playerId)
            if (playerIdx > -1) {
                this.players.splice(playerIdx, 1)
            }
        },
        updateSyncNodes(playerEvent: Player) {
            let playerIdx = this.syncNodes
                .findIndex((player: Player) => player.playerId === playerEvent.playerId)
            if (playerIdx > -1) {
                this.syncNodes[playerIdx] = playerEvent
            } else {
                this.syncNodes.push(playerEvent)
            }
        }
    },
    getters: {
        currentTitle(): string {
            if (this.players.length < 1) {
                return "n/a"
            }
            return `${this.players[0].artist}: ${this.players[0].title} [${this.players[0].remoteTitle}]`
        },
        playerName: (state) => {
            return (playerId: string) => {
                let buffer = [] as string[]
                buffer.push(state.players
                    ?.find((player: Player) => player.playerId === playerId)
                    ?.playerName
                    ?? 'n/a')
                state.syncNodes.forEach((syncNode) => buffer.push(syncNode.playerName ?? 'n/a'))
                return buffer.join(' & ') as string
            }

        }
    }
})

function parseArtworkUrl(lmsArtworkUrl?: string) {
    if (!lmsArtworkUrl) return lmsArtworkUrl

    if (lmsArtworkUrl.startsWith("/imageproxy/") || lmsArtworkUrl.startsWith("/plugins/") || lmsArtworkUrl.startsWith("html/")) {
        return `https://lms.unividuell.org` + ((lmsArtworkUrl.startsWith('/') ? '' : '/')) + lmsArtworkUrl
    } else {
        return lmsArtworkUrl
    }
}

function mapPlayerEvent(playerId: string, playerEvent: PlayerCometD): Player {
    let player : Player =
    {
        playerId : playerId,
        playerName : playerEvent.player_name,
        title : playerEvent.remoteMeta?.title,
        artist : playerEvent.remoteMeta?.artist,
        remoteTitle : playerEvent.remoteMeta?.remote_title,
        artworkUrl : parseArtworkUrl(playerEvent.remoteMeta?.artwork_url),
        mode : playerEvent.mode,
        mixerVolume : playerEvent["mixer volume"],
        connected : playerEvent.player_connected === 1,
        ipAddress : playerEvent.player_ip,
        syncController : playerEvent.sync_master,
        syncNodes : playerEvent.sync_slaves?.split(",") ?? [],
    }
    return player
}

