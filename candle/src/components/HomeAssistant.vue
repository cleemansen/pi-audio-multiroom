<script setup lang="ts">
import { useHomeAssistantClient } from "@/composables/HomeAssistantClient";
import { computed } from "vue";

const { homeAssistantMediaPlayers } = useHomeAssistantClient();
</script>

<template>
  {{ homeAssistantMediaPlayers.size }}
</template>

<style scoped></style>
