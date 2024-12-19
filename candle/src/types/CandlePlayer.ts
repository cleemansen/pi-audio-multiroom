export enum PlayerMode {
  PLAY = "play",
  PAUSE = "pause",
  STOP = "stop",
}
export interface CandlePlayer {
  playerId: string;
  playerName: string;
  mixerVolume: number;
  artworkUrl: string;
  artist: string;
  title: string;
  mode: PlayerMode;
}
export interface CandleLmsPlayer extends CandlePlayer {
  ipAddress: string;
}
