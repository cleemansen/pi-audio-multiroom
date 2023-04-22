export interface Player {
  playerId: string;
  playerName?: string;
  title?: string;
  artist?: string;
  remoteTitle?: string;
  artworkUrl: string | null;
  mode?: string;
  mixerVolume?: number;
  connected?: boolean;
  ipAddress?: string;
  syncController?: string;
  syncNodes: string[];
}

export interface PlayerCometD {
  player_name?: string;
  sync_master?: string;
  sync_slaves?: string;
  current_title?: string;
  remoteMeta?: RemoteMetaCometD;
  mode?: string;
  "mixer volume"?: number;
  player_connected?: number;
  player_ip?: string;
}

export interface PlayerServerstatusCometD {
  playerid: string;
  name: string;
  power: boolean;
  connected: boolean;
  canpoweroff: boolean;
}

export interface RemoteMetaCometD {
  title?: string;
  artist?: string;
  remote_title?: string;
  // GET /plugins/AppGallery/html/images/icon.png HTTP/1.1
  artwork_url?: string;
  bitrate?: string;
}
