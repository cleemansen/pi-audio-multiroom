import {
  getAuth,
  createConnection,
  subscribeEntities,
  HassEntities,
} from "home-assistant-js-websocket";
import { Connection } from "home-assistant-js-websocket/dist/connection";
import { Auth, AuthData } from "home-assistant-js-websocket/dist/auth";
import { onMounted, ref } from "vue";

export interface HomeAssistantMediaPlayerGroupMember {
  id: string;
}

export interface HomeAssistantMediaPlayerAttributes {
  active_queue: string;
  app_id: string;
  device_class: string;
  entity_picture: string;
  entity_picture_local: string;
  friendly_name: string;
  group_members: Array<HomeAssistantMediaPlayerGroupMember>;
  icon: string;
  is_volume_muted: boolean;
  mass_player_type: string;
  media_album_name: string;
  media_artist: string;
  media_content_id: string;
  media_content_type: string;
  media_duration: number;
  media_position: number;
  media_position_updated_at: string;
  media_title: string;
  repeat: string;
  shuffle: boolean;
  supported_features: number;
  volume_level: number;
}

export interface HomeAssistantMediaPlayer {
  attributes: Record<string, HomeAssistantMediaPlayerAttributes>;
  context: Record<string, any>;
  entity_id: string;
  last_changed: string;
  last_updated: string;
  state: string;
}

export const useHomeAssistantClient = () => {
  const connection = ref<Connection>();
  const auth = ref<Auth>();
  const homeAssistantMediaPlayers =
    ref<Record<string, HomeAssistantMediaPlayer>>();

  onMounted(async () => {
    connection.value = await connect();
    if (connection.value) {
      clearUrl();
      subscribeHomeAssistantEntities(connection.value);
    }
  });

  async function connect() {
    const hassUrl = import.meta.env.VITE_HASS_HOST;
    const authOptions = {
      hassUrl,
      async loadTokens(): Promise<AuthData | null | undefined> {
        console.log("load");
        try {
          return JSON.parse(localStorage.hassTokens);
        } catch (err) {
          console.error(err);
          return undefined;
        }
      },
      saveTokens: (tokens: AuthData | null) => {
        console.log("save", tokens);
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
      entitySubscriptionCallback(entities)
    );
  }

  const entitySubscriptionCallback = (entities: HassEntities) => {
    console.log(entities);
    const _mediaPlayers = Object.keys(entities)
      .filter((key) => entities[key].attributes.app_id === "music_assistant")
      .reduce((obj, key) => {
        obj[key] = entities[key];
        return obj;
      }, {});
    // console.log(_mediaPlayers);
    homeAssistantMediaPlayers.value = _mediaPlayers;
    // for (const entityId in _mediaPlayers) {
    //   console.log(entityId);
    // }
  };

  return {
    auth,
    connection,
    homeAssistantMediaPlayers,
    connect,
    subscribeHomeAssistantEntities,
  };
};
