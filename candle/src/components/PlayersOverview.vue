<template>
  <v-container>
    <v-row>
      <v-col
        cols="12"
        sm="6"
        md="6"
        v-for="player in store.players"
        v-bind:key="player.playerId"
      >
        <v-card class="mb-6">
          <v-system-bar>
            <v-icon icon="mdi-cast-audio" class="me-1"></v-icon>
            <span>candle for {{ store.playerName(player.playerId) }}</span>
            <v-spacer></v-spacer>
          </v-system-bar>
          <v-toolbar color="white" class="elevation-2">
            <v-app-bar-nav-icon></v-app-bar-nav-icon>
            <v-spacer />

            <v-btn
              icon="mdi-volume-medium"
              x-large
              @click="volumeStepDown(player)"
            >
            </v-btn>
            <v-btn
              :icon="playPauseIcon(player)"
              x-large
              @click="togglePlayPause(player)"
              :loading="!reachedDesiredMode[player.playerId]"
            >
            </v-btn>
            <v-btn icon="mdi-volume-high" x-large @click="volumeStepUp(player)">
            </v-btn>

            <v-spacer />
            <v-btn icon="mdi-power" @click="shutdown(player)"> </v-btn>
          </v-toolbar>
          <v-row>
            <v-col cols="12" class="pb-4">
              <PlayerVolume
                :key="player.playerId"
                :player-id="player.playerId"
                :player-name="player.playerName"
                :mixer-volume="player.mixerVolume"
                v-on:desired-volume="volumeChange"
              />
              <PlayerVolume
                v-for="node in store.syncNodes"
                v-bind:key="node.playerId"
                :player-id="node.playerId"
                :player-name="node.playerName"
                :mixer-volume="node.mixerVolume"
                v-on:desired-volume="volumeChange"
              />
            </v-col>
          </v-row>
          <v-img :src="player.artworkUrl"> </v-img>
          <v-card-text class="text-black">
            <CurrentTitle :artist="player.artist" :title="player.title" />
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>
<script lang="ts">
import { defineComponent, ref } from "vue";
import { useLmsStore } from "@/stores/LmsStore";
import CurrentTitle from "./CurrentTitle.vue";
import PlayerVolume from "@/components/PlayerVolume.vue";
import type { Player } from "@/types/Player";

export default defineComponent({
  components: { CurrentTitle, PlayerVolume },
  setup() {
    const loading = ref(true);
    const store = useLmsStore();
    const desiredState = [] as Player[];

    return {
      loading,
      store,
      desiredState,
    };
  },
  methods: {
    volumeChange(playerId: string, desiredVolume: number) {
      this.store.volume(playerId, desiredVolume);
    },
    volumeStepUp(player: Player) {
      this.store.volumeStepUp(player.playerId);
    },
    volumeStepDown(player: Player) {
      this.store.volumeStepDown(player.playerId);
    },
    togglePlayPause(player: Player) {
      this.store.togglePlayPause(player.playerId);
    },
    shutdown(player: Player) {
      console.debug(player);
    },
    playPauseIcon(player: Player): string {
      if (player.mode === "play") {
        return "mdi-pause";
      } else if (player.mode === "pause" || player.mode === "stop") {
        return "mdi-play";
      }
      return "mdi-heart-broken";
    },
  },
  computed: {
    reachedDesiredMode(playerId: string): boolean {
      if (
        this.desiredState.find(
          (desiredStatePlayer) => desiredStatePlayer.playerId == playerId
        )?.mode == null
      ) {
        // we are not waiting for a desired mode
        return true;
      }

      const storedPlayer = this.store.players?.find(
        (player) => player.playerId === playerId
      );
      if (storedPlayer?.mode === undefined) {
        // we are not waiting for a desired mode
        return true;
      }
      return (
        this.desiredState.find(
          (desiredStatePlayer) => desiredStatePlayer.playerId == playerId
        )?.mode === storedPlayer.mode
      );
    },
  },
});
</script>

<style>
:root {
  --va-app-bar-height: 1.2rem;
}
</style>
