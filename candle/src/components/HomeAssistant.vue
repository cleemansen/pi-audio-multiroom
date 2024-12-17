<script setup lang="ts">
import { useHomeAssistantClient } from "@/composables/HomeAssistantClient";
import { computed } from "vue";

const { mediaPlayers } = useHomeAssistantClient();

const players = computed(() => {
  if (mediaPlayers.value) {
    return Object.values(mediaPlayers.value).map((value) => ({
      name: value?.attributes?.friendly_name || "n/a",
      source: value?.attributes?.source,
    }));
  }
  return [];
  // if (mediaPlayers.value) {
  //   return new Map(Object.entries(mediaPlayers.value));
  // }
  // return [];
});
</script>

<template>
  <ul>
    <li v-for="(player, index) in players" :key="index">
      {{ player.name }}
      <ul>
        <li>{{ player.source }}</li>
      </ul>
    </li>
  </ul>
</template>

<style scoped></style>
