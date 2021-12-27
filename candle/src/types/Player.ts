export interface Player {
    playerId: string,
    playerName?: string,
    title?: string,
    artist?: string,
    remoteTitle?: string,
    artworkUrl?: string,
    mode?: string,
    mixerVolume?: number,
    connected?: boolean,
    ipAddress?: string,
    syncController?: string,
    syncNodes: string[]
}