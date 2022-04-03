<template>
  <v-progress-linear
    v-bind:key="playerId"
    v-model="vol"
    :stream="desiredVolume >= 0"
    height="14"
    color="indigo"
    class="mt-1 elevation-4"
  >
    <template v-slot:default="{ value }">
      <div class="pt-2 pb-2" style="color: white; font-size: 14px">
        {{ playerName }} {{ Math.ceil(value) }}
      </div>
    </template>
  </v-progress-linear>
</template>

<script lang="ts">
import { defineComponent, ref } from "vue";

export default defineComponent({
  props: { playerId: String, playerName: String, mixerVolume: Number },
  setup() {
    const loading = ref(false);
    const desiredVolume = ref(-1);

    return {
      loading,
      desiredVolume,
    };
  },
  computed: {
    vol: {
      get: function () {
        return this.mixerVolume;
      },
      set: function (newValue: number) {
        this.desiredVolume = Math.ceil(newValue);
      },
    },
  },
  watch: {
    desiredVolume: function (val: number) {
      if (val) {
        this.$emit("desired-volume", this.playerId, val);
      }
    },
    mixerVolume: function (val: number) {
      if (this.desiredVolume === val) {
        this.desiredVolume = -1;
      }
    },
  },
});
</script>

<style scoped></style>
