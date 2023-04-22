<template>
  <v-slider
    v-bind:key="playerId"
    v-model="vol"
    hide-details
    :color="stateColor"
    :thumb-label="true"
    elevation="4"
    class="px-2"
  >
    <template v-slot:append
      ><div style="width: 50px" class="text-caption text-end">
        {{ playerName }}
      </div></template
    >
  </v-slider>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";

const props = defineProps({
  playerId: String,
  playerName: String,
  mixerVolume: Number,
});
const emit = defineEmits(["desired-volume"]);

const desiredVolume = ref(-1);
const vol = computed({
  get: () => {
    if (desiredVolume.value > -1) {
      return desiredVolume.value;
    }
    return props.mixerVolume;
  },
  set: (newValue: number) => {
    desiredVolume.value = newValue;
    emit("desired-volume", props.playerId, newValue);
  },
});
const stateColor = computed(() =>
  desiredVolume.value > -1 ? "purple" : "indigo"
);

watch(
  () => props.mixerVolume,
  (val: number) => {
    console.info(`mixer-vol update: ${val} (desired is ${desiredVolume.value})`)
    if (Math.ceil(desiredVolume.value) === Math.ceil(val)) {
      desiredVolume.value = -1;
    }
  }
);
</script>

<style scoped></style>
