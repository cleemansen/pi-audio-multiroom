<template>
  <div>
    <p ref="currentSong" class="display-1 text--primary currentSong">
      <v-icon class="lookupAction" size="200" color="#1ED760" v-if="lookupAction">mdi-spotify</v-icon>
      <span v-for="(word, index) in currentSong" :key="index" v-html="word" v-on:click="wordTouch"></span>
    </p>
  </div>
</template>

<script>
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
      let words = context
          // kudos: https://stackoverflow.com/a/18473490/810944
          .replace(/([ .,;]+)/g, '$1§sep§').split('§sep§')
      console.log(words)
      return words
    },
    wordTouch(element) {
      // kudos: https://stackoverflow.com/a/51921785/810944
      element.target.classList.toggle('lookup')
      let selected = this.$refs.currentSong.querySelectorAll('.lookup')
      // map the node-list: kudos: https://stackoverflow.com/a/32767009/810944
      this.selection = Array.from(selected, (item) => item.innerHTML).join("")
    }
  },
}
</script>

<style scoped>
.currentSong {
  position: relative;
}

.lookup {
  background-color: #1ED760;
  color: #2941ab;
}

.lookupAction {
  position: fixed !important;
  top: 20%;
  left: -80px;
  z-index: 20;
}
</style>