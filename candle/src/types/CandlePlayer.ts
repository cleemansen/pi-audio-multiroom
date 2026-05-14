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
  artworkUrl: string | undefined | null;
  artist: string | undefined | null;
  title: string | undefined | null;
  mode: PlayerMode;
}
export interface CandleLmsPlayer extends CandlePlayer {
  ipAddress: string;
}
export interface CandleHomeAssistantPlayer extends CandlePlayer {
  album: string | undefined | null;
  active_queue: string;
  followers: CandleHomeAssistantPlayer[];
}
