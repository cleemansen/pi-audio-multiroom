<template>
  <v-row>
    <v-col cols="12" class="pt-1 pb-1">
      <v-progress-linear
          v-for="node in volumes" v-bind:key="node.playerId"
          v-model="node.mixerVolume"
          height="14"
          color="purple"
          class="mt-1 elevation-4">
        <template v-slot:default="{ value }">
          {{ node.playerName }} {{ Math.ceil(value) }}
        </template>
      </v-progress-linear>
    </v-col>
  </v-row>
</template>

<script>
export default {
  name: "PlayerVolume",
  props: ['player', 'nodes'],
  computed: {
    volumes() {
      let p = {}
      p[this.player.playerId] = this.player
      let player = this.objectMap(p, player => {
        return {
          playerId: player.playerId,
          playerName: player.playerName,
          mixerVolume: player.mixerVolume
        }
      })
      let nodes = this.objectMap(this.nodes[this.player.playerId], node => {
        return {
          playerId: node.playerId,
          playerName: node.playerName,
          mixerVolume: node.mixerVolume
        }
      })
      // kudos: https://stackoverflow.com/a/21450110/810944
      return {
        ...player,
        ...nodes
      }
    }
  },
  watch: {
    volumes: {
      handler: function (val) {
        console.log(val)
      },
      deep: true
    }
  }

}
</script>

<style scoped>

</style>