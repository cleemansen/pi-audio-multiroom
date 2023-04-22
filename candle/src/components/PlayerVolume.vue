<template>
  <v-slider
    v-bind:key="playerId"
    v-model="vol"
    hide-details
    :color="stateColor"
    :thumb-label="true"
    elevation="4"
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

const progress = ref(false);
const desiredVolume = ref<number>(props.mixerVolume);

const vol = computed({
  get: () => desiredVolume.value,
  set: (newValue: number) => {
    progress.value = true;
    desiredVolume.value = Math.ceil(newValue);
  },
});
const stateColor = computed(() => (progress.value ? "purple" : "indigo"));

watch(desiredVolume, (val: number) => {
  if (val) {
    emit("desired-volume", props.playerId, val);
  }
});
watch(
  () => props.mixerVolume,
  (val: number) => {
    if (desiredVolume.value === val) {
      progress.value = false;
    }
  }
);
</script>

<style scoped></style>
