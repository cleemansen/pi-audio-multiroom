import {
  createConnection,
  getAuth,
  HassEntities,
  HassEntity,
  MessageBase,
  subscribeEntities,
} from "home-assistant-js-websocket";
import { Connection } from "home-assistant-js-websocket/dist/connection";
import { Auth, AuthData } from "home-assistant-js-websocket/dist/auth";
import { computed, onMounted, ref } from "vue";
import { CandleHomeAssistantPlayer, PlayerMode } from "../types/CandlePlayer";

export interface HomeAssistantMediaPlayerGroupMember {
  id: string;
}

export interface HomeAssistantMediaPlayerAttributes {
  active_queue: string;
  app_id: string;
  device_class: string;
  entity_picture: string | undefined | null;
  entity_picture_local: string | undefined | null;
  friendly_name: string;
  group_members: Array<HomeAssistantMediaPlayerGroupMember>;
  icon: string;
  is_volume_muted: boolean;
  mass_player_type: string;
  media_album_name: string | undefined | null;
  media_artist: string | undefined | null;
  media_content_id: string;
  media_content_type: string;
  media_duration: number;
  media_position: number;
  media_position_updated_at: string;
  media_title: string | undefined | null;
  repeat: string;
  shuffle: boolean;
  supported_features: number;
  volume_level: number;
}

export type HomeAssistantMediaPlayer = HassEntity & {
  attributes: HomeAssistantMediaPlayerAttributes;
};

export const useHomeAssistantClient = () => {
  const connection = ref<Connection>();
  const auth = ref<Auth>();
  // all media players returned by HA
  const homeAssistantMediaPlayers = ref<HomeAssistantMediaPlayer[]>([]);
  // groups of media players grouped by same `active_queue`
  const groupsBtActiveQueueId = computed(() => {
    const byActiveQueue = new Map<string, Array<HomeAssistantMediaPlayer>>();
    homeAssistantMediaPlayers.value.forEach((player) => {
      if (byActiveQueue.has(player.attributes.active_queue)) {
        byActiveQueue.get(player.attributes.active_queue)?.push(player);
      } else {
        byActiveQueue.set(player.attributes.active_queue, [player]);
      }
    });
    return byActiveQueue;
  });

  const candlePlayers = computed<CandleHomeAssistantPlayer[]>(() => {
    const electedLeaderInGroup = new Map<string, HomeAssistantMediaPlayer>();
    for (const [queueId, player] of groupsBtActiveQueueId.value) {
      // all players in a group share the same attributes we are interested in (like album name etc.),
      // so simply take the first player in each group to access this information
      const firstInGroup = player[0];
      electedLeaderInGroup.set(queueId, firstInGroup);
    }
    return Array.from(electedLeaderInGroup).map(([queueId, leader]) => {
      const combinedName =
        groupsBtActiveQueueId.value
          .get(queueId)
          ?.map((player) => playerName(player))
          ?.join(" & ") ?? "n/a";
      return mapToCandle(
        leader,
        combinedName,
        // followers are all players in the group - including the leader (needed for eg the separate volume control)
        groupsBtActiveQueueId.value.get(queueId) ?? [],
      );
    });
  });

  function mapToCandle(
    player: HomeAssistantMediaPlayer,
    name: string,
    followers: HomeAssistantMediaPlayer[],
  ): CandleHomeAssistantPlayer {
    return {
      playerId: player.entity_id,
      playerName: name,
      artist: player.attributes.media_artist,
      title: player.attributes.media_title,
      album: player.attributes.media_album_name,
      mixerVolume: player.attributes.volume_level * 100,
      artworkUrl: player.attributes.entity_picture,
      mode: player.state as PlayerMode,
      active_queue: player.attributes.active_queue,
      followers: followers.map((follower) =>
        mapToCandle(follower, playerName(follower), []),
      ),
    };
  }

  function playerName(player: HomeAssistantMediaPlayer) {
    if ((player.attributes.friendly_name.match(/:/g) || []).length >= 5) {
      // do not return s/t like `squeezeplay: 24:05:0f:95:46:70`
      return player.entity_id.replace("media_player.", "");
    } else {
      return player.attributes.friendly_name;
    }
  }

  onMounted(async () => {
    connection.value = await connect();
    if (connection.value) {
      clearUrl();
      subscribeHomeAssistantEntities(connection.value);
    }
  });

  function volume(playerId: string, desiredVolume: number) {
    const message: MessageBase = {
      type: "call_service",
      domain: "media_player",
      service: "volume_set",
      return_response: false,
      service_data: {
        entity_id: playerId,
        volume_level: desiredVolume / 100,
      },
    };
    connection.value?.sendMessage(message);
  }

  function volumeStepUp(playerId: string) {
    const message: MessageBase = {
      type: "call_service",
      domain: "media_player",
      service: "volume_up",
      return_response: false,
      service_data: {
        entity_id: playerId,
      },
    };
    connection.value?.sendMessage(message);
  }

  function volumeStepDown(playerId: string) {
    const message: MessageBase = {
      type: "call_service",
      domain: "media_player",
      service: "volume_down",
      return_response: false,
      service_data: {
        entity_id: playerId,
      },
    };
    connection.value?.sendMessage(message);
  }

  function togglePlayPause(playerId: string) {
    const message: MessageBase = {
      type: "call_service",
      domain: "media_player",
      service: "media_play_pause",
      return_response: false,
      service_data: {
        entity_id: playerId,
      },
    };
    connection.value?.sendMessage(message);
  }

  async function connect() {
    const hassUrl = import.meta.env.VITE_HASS_HOST;
    const authOptions = {
      hassUrl,
      async loadTokens(): Promise<AuthData | null | undefined> {
        try {
          return JSON.parse(localStorage.hassTokens);
        } catch (err) {
          console.error(err);
          return undefined;
        }
      },
      saveTokens: (tokens: AuthData | null) => {
        localStorage.hassTokens = JSON.stringify(tokens);
      },
    };
    try {
      // Try to pick up authentication after user logs in
      auth.value = await getAuth(authOptions);
    } catch (err) {
      console.error(`Unknown error: ${err}`);
      return;
    }
    try {
      return await createConnection({ auth: auth.value });
    } catch (e) {
      console.error("Cannot create connection:", e);
      console.info("Clearing local tokens..");
      localStorage.hassTokens = null;
      return undefined;
    }
  }

  function clearUrl() {
    // Clear url if we have been able to establish a connection
    if (location.search.includes("auth_callback=1")) {
      history.replaceState(null, "", location.pathname);
    }
  }

  function subscribeHomeAssistantEntities(connection: Connection) {
    subscribeEntities(connection, (entities) =>
      entitySubscriptionCallback(entities),
    );
  }

  const entitySubscriptionCallback = (entities: HassEntities) => {
    homeAssistantMediaPlayers.value = Object.entries(entities)
      .filter(([, value]) => value.attributes.app_id === "music_assistant")
      .map(([, value]) => value as HomeAssistantMediaPlayer);
  };

  return {
    auth,
    connection,
    homeAssistantMediaPlayers,
    candlePlayers,
    volume,
    volumeStepUp,
    volumeStepDown,
    togglePlayPause,
    connect,
    subscribeHomeAssistantEntities,
  };
};
