<template>
  <v-progress-linear
      v-bind:key="playerId"
      v-model="vol"
      :stream="desiredVolume !== null"
      height="14"
      color="indigo"
      class="mt-1 elevation-4">
    <template v-slot:default="{ value }">
      <div class="pt-2 pb-2" style="color: white; font-size: 14px">{{ playerName }} {{ Math.ceil(value) }}</div>
    </template>
  </v-progress-linear>
</template>

<script>
export default {
  name: "PlayerVolume",
  props: ['playerId', 'playerName', 'mixerVolume'],
  data: () => ({
    desiredVolume: null,
    loading: false
  }),
  computed: {
    vol: {
      get: function () {
        return this.mixerVolume
      },
      set: function (newValue) {
        this.desiredVolume = Math.ceil(newValue)
      }
    }
  },
  watch: {
    desiredVolume: function (val) {
      if (val) {
        this.$emit('desired-volume', this.playerId, val)
      }
    },
    mixerVolume: function (val) {
      if (this.desiredVolume === val) {
        this.desiredVolume = null
      }
    }
  }

}
</script>

<style scoped>

</style>