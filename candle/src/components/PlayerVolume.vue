<template>
  <v-slider
    v-bind:key="playerId"
    v-model="model"
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
import { computed, ref } from "vue";

export interface Props {
  playerId: string;
  playerName: string | null;
}
defineProps<Props>();

const model = defineModel<number>({ required: true });

// not in use anymore - HA does response very quickly and w/ good quality :)
const desiredVolume = ref(-1);
const stateColor = computed(() =>
  desiredVolume.value > -1 ? "purple" : "indigo"
);
</script>

<style scoped></style>
