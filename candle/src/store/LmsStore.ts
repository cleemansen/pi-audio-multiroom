import {defineStore} from "pinia";
import {LmsCometDRepository} from "../repo/LmsCometDRepository";
import {Player, PlayerCometD} from "../types/Player";
import {Message} from "cometd";

export const useLmsStore = defineStore('playerStatus', {
    state: () => ({
        players: [] as Player[],
        cometD: new LmsCometDRepository(),
    }),
    actions: {
        updatePlayer(msg: Message) {
            let playerIdx = this.players
                .findIndex((player: Player) => player.playerId === msg.channel.substring(msg.channel.lastIndexOf('/') + 1))
            let playerCometD = (msg.data as PlayerCometD)
            this.$patch((state) => {
                state.players[playerIdx].playerName = playerCometD.player_name
                state.players[playerIdx].title = playerCometD.remoteMeta?.title
                state.players[playerIdx].artist = playerCometD.remoteMeta?.artist
                state.players[playerIdx].remoteTitle = playerCometD.remoteMeta?.remote_title
                state.players[playerIdx].artworkUrl = playerCometD.remoteMeta?.artwork_url
                state.players[playerIdx].mode = playerCometD.mode
                state.players[playerIdx].mixerVolume = playerCometD["mixer volume"]
                state.players[playerIdx].connected = playerCometD.player_connected === 1
                state.players[playerIdx].ipAddress = playerCometD.player_ip
                state.players[playerIdx].syncController = playerCometD.sync_master
                state.players[playerIdx].syncNodes = playerCometD.sync_slaves?.split(",") ?? []
            })
        },
    },
    getters: {
        currentTitle(): string {
            if (this.players.length > 0) {
                return this.players[0].remoteTitle ?? "n/a"
            }
            return "n/a"
        }
    }
})

