<template>
  <v-app>
    <v-main>
      <PlayersOverview/>
    </v-main>
  </v-app>
</template>

<script>
import PlayersOverview from "@/components/PlayersOverview";

export default {
  name: 'App',

  components: {
    PlayersOverview,
  },

  data: () => ({
    //
  }),
};
</script>
