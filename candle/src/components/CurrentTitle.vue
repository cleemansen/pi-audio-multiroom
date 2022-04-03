<template>
  <div>
    <p ref="currentSong" class="text-h3 currentSong text-high-emphasis">
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

<script lang="ts">
import { defineComponent } from "vue";
import { isMobile } from "mobile-device-detect";

export default defineComponent({
  props: {
    artist: String,
    title: String,
  },
  data() {
    return {
      selection: null as string | null,
    };
  },
  computed: {
    currentSong() {
      let currentSong = "";
      if (this.artist) {
        currentSong += this.artist;
      }
      if (this.title) {
        if (currentSong !== "") {
          currentSong += " — ";
        }
        currentSong += this.title;
      }
      return this.splitter(currentSong);
    },
    lookupAction() {
      return this.selection !== null && this.selection !== "";
    },
  },
  methods: {
    splitter(context: string) {
      return (
        context
          // kudos: https://stackoverflow.com/a/18473490/810944
          .replace(/([ .,;]+)/g, "$1§sep§")
          .split("§sep§")
      );
    },
    wordTouch(element: any) {
      // kudos: https://stackoverflow.com/a/51921785/810944
      element.target?.classList?.toggle("selectedSongWord");
      const selected = (this.$refs.currentSong as Element).querySelectorAll(
        ".selectedSongWord"
      );
      // map the node-list: kudos: https://stackoverflow.com/a/32767009/810944
      this.selection = Array.from(
        selected,
        (item: Element) => item.innerHTML
      ).join("");
    },
    lookup() {
      if (!this.selection) {
        return;
      }
      if (isMobile) {
        // mobile
        window.open("spotify:search:" + encodeURI(this.selection));
      } else {
        // browser
        window.open(
          "https://open.spotify.com/search/" + encodeURI(this.selection)
        );
      }
    },
  },
});
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
