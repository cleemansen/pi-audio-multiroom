export enum PlayerMode {
  PLAY = "play",
  PLAYING = "playing",
  PAUSE = "pause",
  PAUSED = "paused",
  STOP = "stop",
  IDLE = "idle",
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
export interface CandleHomeAssistantPlayer extends CandlePlayer {
  album: string;
  active_queue: string;
}
