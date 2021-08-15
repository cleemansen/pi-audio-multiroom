<template>
  <div>
    <p ref="currentSong" class="display-1 text--primary currentSong">
      <v-icon class="lookupAction" size="200" color="#1ED760" v-if="lookupAction" v-on:click="lookup">mdi-spotify
      </v-icon>
      <span v-for="(word, index) in currentSong" :key="index" v-html="word" v-on:click="wordTouch"
            class="songWord"></span>
    </p>
  </div>
</template>

<script>
import {isMobile} from 'mobile-device-detect';

export default {
  name: "CurrentTitle",
  props: ['artist', 'title'],
  data: () => ({
    selection: null
  }),
  computed: {
    currentSong() {
      let currentSong = ""
      if (this.artist) {
        currentSong += this.artist
      }
      if (this.title) {
        if (currentSong !== "") {
          currentSong += " — "
        }
        currentSong += this.title
      }
      return this.splitter(currentSong)
    },
    lookupAction() {
      return this.selection !== null && this.selection !== ""
    }
  },
  methods: {
    splitter(context) {
      return context
          // kudos: https://stackoverflow.com/a/18473490/810944
          .replace(/([ .,;]+)/g, '$1§sep§').split('§sep§')
    },
    wordTouch(element) {
      // kudos: https://stackoverflow.com/a/51921785/810944
      element.target.classList.toggle('selectedSongWord')
      let selected = this.$refs.currentSong.querySelectorAll('.selectedSongWord')
      // map the node-list: kudos: https://stackoverflow.com/a/32767009/810944
      this.selection = Array.from(selected, (item) => item.innerHTML).join("")
    },
    lookup() {
      if (isMobile) {
        // mobile
        window.open("spotify:search:" + encodeURI(this.selection))
      } else {
        // browser
        window.open("https://open.spotify.com/search/" + encodeURI(this.selection))
      }
    }
  },
}
</script>

<style scoped>
.currentSong {
  position: relative;
}

.songWord {
  cursor: zoom-in;
}

.selectedSongWord {
  background-color: #1ED760;
  color: #2941ab;
  cursor: zoom-out;
}

.lookupAction {
  position: fixed !important;
  top: 20%;
  left: -80px;
  z-index: 20;
}
</style>