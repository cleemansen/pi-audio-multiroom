<template>
  <v-container>
    <v-row>
      <v-col
        v-for="player in candlePlayers"
        :key="player.playerId"
        cols="12"
        md="6"
      >
        <v-card class="mb-6" :loading="shutdownInitialized">
          <v-toolbar class="elevation-2">
            <v-app-bar-nav-icon></v-app-bar-nav-icon>
            <v-toolbar-title>{{ player.playerName }}</v-toolbar-title>

            <v-btn
              icon="mdi-volume-medium"
              x-large
              @click="volumeStepDown(player.playerId)"
            >
            </v-btn>
            <v-btn
              :icon="playPauseIcon(player.mode)"
              x-large
              :loading="!reachedDesiredMode(player.playerId)"
              @click="togglePlayPause(player.playerId)"
            >
            </v-btn>
            <v-btn
              icon="mdi-volume-high"
              x-large
              @click="volumeStepUp(player.playerId)"
            >
            </v-btn>

            <!--            <v-btn icon="mdi-power" @click="shutdown(player)"> </v-btn>-->
          </v-toolbar>
          <v-row>
            <v-col cols="12" class="pb-4">
              <PlayerVolume
                v-for="node in player.followers"
                :key="node.playerId"
                :player-id="node.playerId"
                :player-name="node.playerName"
                :model-value="node.mixerVolume"
                @update:model-value="
                  ($event: number) => volumeChange(node.playerId, $event)
                "
              />
            </v-col>
          </v-row>
          <v-img v-if="player.artworkUrl" :src="player.artworkUrl"> </v-img>
          <v-card-text class="text-black">
            <CurrentTitle
              :artist="player.artist"
              :title="player.title"
              :album="player.album"
            />
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>
<script setup lang="ts">
import { ref } from "vue";
import CurrentTitle from "./CurrentTitle.vue";
import PlayerVolume from "../components/PlayerVolume.vue";
import { CandlePlayer, PlayerMode } from "../types/CandlePlayer";
import { useHomeAssistantClient } from "../composables/HomeAssistantClient";

const { candlePlayers, volume, volumeStepUp, volumeStepDown, togglePlayPause } =
  useHomeAssistantClient();
const desiredState = ref<CandlePlayer[]>([]);
const shutdownInitialized = ref(false);

function volumeChange(playerId: string, desiredVolume: number) {
  volume(playerId, desiredVolume);
}
function playPauseIcon(mode: PlayerMode): string {
  if (mode === PlayerMode.PLAY || mode === PlayerMode.PLAYING) {
    return "mdi-pause";
  } else if (
    mode === PlayerMode.PAUSE ||
    mode === PlayerMode.PAUSED ||
    mode === PlayerMode.STOP ||
    mode === PlayerMode.IDLE
  ) {
    return "mdi-play";
  }
  return "mdi-heart-broken";
}
function reachedDesiredMode(playerId: string): boolean {
  if (
    desiredState.value.find(
      (desiredStatePlayer) => desiredStatePlayer.playerId == playerId,
    )?.mode == null
  ) {
    // we are not waiting for a desired mode
    return true;
  }

  const storedPlayer = candlePlayers.value.find(
    (player: CandlePlayer) => player.playerId === playerId,
  );
  if (storedPlayer?.mode === undefined) {
    // we are not waiting for a desired mode
    return true;
  }
  return (
    desiredState.value.find(
      (desiredStatePlayer) => desiredStatePlayer.playerId == playerId,
    )?.mode === storedPlayer.mode
  );
}
</script>

<style>
:root {
  --va-app-bar-height: 1.2rem;
}
</style>
