export interface Player {
  playerId: string;
  playerName: string | null;
  title: string | null;
  artist: string | null;
  remoteTitle: string | null;
  artworkUrl: string | null;
  mode: string | null;
  mixerVolume: number | null;
  connected: boolean | null;
  ipAddress: string | null;
  syncController: string | null;
  syncNodes: string[];
}

export interface PlayerCometD {
  player_name: string | null;
  sync_master: string | null;
  sync_slaves: string | null;
  current_title: string;
  remoteMeta: RemoteMetaCometD | null;
  mode: string | null;
  "mixer volume": number | null;
  player_connected: number | null;
  player_ip: string | null;
}

export interface PlayerServerstatusCometD {
  playerid: string;
  name: string;
  power: boolean;
  connected: boolean;
  canpoweroff: boolean;
}

export interface RemoteMetaCometD {
  title: string | null;
  artist: string | null;
  remote_title: string | null;
  // GET /plugins/AppGallery/html/images/icon.png HTTP/1.1
  artwork_url: string | null;
  bitrate: string | null;
}
