<template>
  <div>
    <p class="display-1 text--primary">
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
    }
  },
  methods: {
    splitter(context) {
      let words = context
          // kudos: https://stackoverflow.com/a/36508315/810944
          .match(/\b(\w+\W+)/g)
      console.log(words)
      return words
    },
    wordTouch(element) {
      // kudos: https://stackoverflow.com/a/51921785/810944
      element.target.classList.toggle('lookup')
    }
  },
  watch: {
    // selection: function (newVal) {
    //   console.log("changed to " + newVal)
    // }
  }
}
</script>

<style scoped>
.lookup {
  color: aquamarine;
}
</style>