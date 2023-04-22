<template>
  <div>
    <p ref="currentSongElement" class="text-h3 currentSong text-high-emphasis">
      <v-icon
        class="lookupAction"
        :size="200"
        color="#1ED760"
        v-if="lookupAction"
        v-on:click="lookup"
        >mdi-spotify</v-icon
      >
      <span
        v-for="(word, index) in currentSong"
        :key="index"
        v-html="word"
        v-on:click="wordTouch"
        class="songWord"
      ></span>
    </p>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable require-jsdoc */
import { computed, ref } from "vue";
import { isMobile } from "mobile-device-detect";

const props = defineProps({
  artist: String,
  title: String,
});
const currentSongElement = ref<Element | null>(null);
const selection = ref<string | null>(null);
const currentSong = computed(() => {
  let currentSong = "";
  if (props.artist) {
    currentSong += props.artist;
  }
  if (props.title) {
    if (currentSong !== "") {
      currentSong += " — ";
    }
    currentSong += props.title;
  }
  return splitter(currentSong);
});
const lookupAction = computed(() => {
  return selection.value !== null && selection.value !== "";
});
function splitter(context: string) {
  return (
    context
      // kudos: https://stackoverflow.com/a/18473490/810944
      .replace(/([ .,;]+)/g, "$1§sep§")
      .split("§sep§")
  );
}
function wordTouch(element: Event) {
  // kudos: https://stackoverflow.com/a/51921785/810944
  (element.target as HTMLInputElement)?.classList?.toggle("selectedSongWord");
  const selected = (currentSongElement.value as Element).querySelectorAll(
    ".selectedSongWord"
  );
  // map the node-list: kudos: https://stackoverflow.com/a/32767009/810944
  selection.value = Array.from(
    selected,
    (item: Element) => item.innerHTML
  ).join("");
}
function lookup() {
  if (!selection.value) {
    return;
  }
  if (isMobile) {
    // mobile
    window.open("spotify:search:" + encodeURI(selection.value!));
  } else {
    // browser
    window.open(
      "https://open.spotify.com/search/" + encodeURI(selection.value!)
    );
  }
}
</script>

<style scoped>
.currentSong {
  position: relative;
  text-align: start;
}

.songWord {
  cursor: zoom-in;
}

.selectedSongWord {
  background-color: #1ed760;
  color: #2941ab;
  cursor: zoom-out;
}

.lookupAction {
  position: fixed !important;
  top: 14%;
  left: -100px;
  z-index: 20;
  color: #1ed760;
  font-size: 16rem;
}
</style>
