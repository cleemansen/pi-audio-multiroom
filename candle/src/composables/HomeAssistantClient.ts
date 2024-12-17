import {
  getAuth,
  createConnection,
  subscribeEntities,
  HassEntities,
} from "home-assistant-js-websocket";
import { Connection } from "home-assistant-js-websocket/dist/connection";
import { Auth, AuthData } from "home-assistant-js-websocket/dist/auth";
import { onMounted, ref } from "vue";

export const useHomeAssistantClient = () => {
  const connection = ref<Connection>();
  const auth = ref<Auth>();
  const mediaPlayers = ref();

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
    mediaPlayers.value = _mediaPlayers;
    // for (const entityId in _mediaPlayers) {
    //   console.log(entityId);
    // }
  };

  return {
    auth,
    connection,
    mediaPlayers,
    connect,
    subscribeHomeAssistantEntities,
  };
};
