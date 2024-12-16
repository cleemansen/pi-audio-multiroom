<script setup lang="ts">
import { useHomeAssistantClient } from "@/composables/HomeAssistantClient";

const { auth, connection } = useHomeAssistantClient();
</script>

<template>
  <p>{{ auth }}</p>
  <p>{{ connection }}</p>
</template>

<style scoped></style>
