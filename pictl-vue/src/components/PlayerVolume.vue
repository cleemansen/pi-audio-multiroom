<template>
  <v-row>
    <v-col cols="12" class="pt-1 pb-1">
      <v-progress-linear
          :value="player.mixerVolume"
          height="14"
          color="amber"
          class="elevation-4">
        <template v-slot:default="{ value }">
          <span class="text-start">{{ player.playerName }} {{ value }}</span>
        </template>
      </v-progress-linear>
      <v-progress-linear
          v-for="node in nodes[player.playerId]" v-bind:key="node.playerId"
          :value="node.mixerVolume"
          height="14"
          color="purple"
          class="mt-1 elevation-4">
        <template v-slot:default="{ value }">
          {{ node.playerName }} {{ value }}
        </template>
      </v-progress-linear>
    </v-col>
  </v-row>
</template>

<script>
export default {
  name: "PlayerVolume",
  props: ['player', 'nodes']
}
</script>

<style scoped>

</style>