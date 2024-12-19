<template>
  <v-app>
    <v-main>
      <PlayersOverview />
    </v-main>
  </v-app>
</template>

<script setup lang="ts">
import PlayersOverview from "./components/PlayersOverview.vue";
import { useTheme } from "vuetify";

const theme = useTheme();

window
  .matchMedia("(prefers-color-scheme: dark)")
  .addEventListener("change", (e) => {
    // kudos: https://stackoverflow.com/a/57795518/810944
    theme.global.name.value = e.matches ? "darkTheme" : "lightTheme";
  });
</script>
