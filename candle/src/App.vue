<template>
  <PlayersOverview />
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import PlayersOverview from "./components/PlayersOverview.vue";

export default defineComponent({
  name: 'App',

  components: {
    PlayersOverview,
  },

})
</script>
