<template>
  <v-slider
    v-bind:key="playerId"
    v-model="vol"
    hide-details
    :color="stateColor"
    :thumb-label="true"
    elevation="4"
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
  get: () => props.mixerVolume,
  set: (newValue: number) => (desiredVolume.value = Math.ceil(newValue)),
});
const progress = computed({
  get: () => desiredVolume.value >= 0,
  set: () => {
    // no-op
  },
});
const stateColor = computed({
  get: () => (progress.value ? "purple" : "indigo"),
  set: () => {
    // no-op
  },
});

watch(desiredVolume, (val: number) => {
  if (val) {
    emit("desired-volume", props.playerId, val);
  }
});
watch(
  () => props.mixerVolume,
  (val: number) => {
    if (desiredVolume.value === val) {
      desiredVolume.value = -1;
    }
  }
);
</script>

<style scoped></style>
