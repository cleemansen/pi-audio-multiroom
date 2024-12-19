<script setup lang="ts">
import { useHomeAssistantClient } from "@/composables/HomeAssistantClient";
import { computed } from "vue";

const { homeAssistantMediaPlayers, groups, groupLeadersOnly, players } =
  useHomeAssistantClient();
</script>

<template>
  <div v-for="player in players" :key="player.entity_id">
    <h1>{{ player.attributes.friendly_name }} [{{ player.entity_id }}]</h1>
    <div>{{ player.state }}</div>
    <div>
      {{ player.attributes.media_artist }} -
      {{ player.attributes.media_title }} [{{
        player.attributes.media_album_name
      }}]
    </div>
  </div>
</template>

<style scoped></style>
