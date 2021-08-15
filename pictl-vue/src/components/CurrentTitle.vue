<template>
  <div>
    <p ref="current-title" id="current-title" class="display-1 text--primary" v-html="currentSong"></p>
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
  mounted() {
    document.onselectionchange = () => {
      let selection = document.getSelection()
      console.log(selection)
      // if (selection.focusOffset != null) {
      //   let context = selection.focusNode.textContent
      //
      //   console.log(words)
      // }
    }
  },
  methods: {
    splitter(context) {
      let words = context
          .replace(/([ .,;]+)/g, '$1§sep§')
          .split('§sep§')
          .map((word) => word.trim())
          .map((word) => {
                let span = document.createElement("span")
                span.textContent = word
                return span
              }
          );
      console.log(words)
      return words
    }
  },
  watch: {
    selection: function (newVal) {
      console.log("changed to " + newVal)
    }
  }
}
</script>

<style scoped>
.lookup {
  color: aquamarine;
}
</style>