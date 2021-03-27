<template>
  <div>
    <p id="current-title" class="display-1 text--primary">{{ currentSong }}</p>
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
      return currentSong
    }
  },
  mounted() {
    document.onselectionchange = () => {
      let selection = document.getSelection()
      console.log(selection)
      if (selection.focusOffset != null) {
        let context = selection.focusNode.textContent
        let selectedWord = "";
        // backwards to first word-splitter
        for (let i = selection.focusOffset - 1; i >= 0; i--) {
          let currentChar = context[i]
          if (currentChar === ' ') {
            break
          }
          selectedWord += context[i]
        }
        selectedWord = selectedWord.split("").reverse().join("")
        // forwards to first word-splitter
        for (let i = selection.focusOffset; i < context.length; i++) {
          let currentChar = context[i]
          if (currentChar === ' ') {
            break
          }
          selectedWord += context[i]
        }
        console.log(selectedWord)

        // let span = document.createElement("span")
        // span.setAttribute("class", "lookup")
        // span.appendChild(document.createTextNode(selectedWord))
        // document.getElementById("current-title").insertBefore(span, selection.focusNode.splitText(selection.focusOffset))
      }
    }
  },
  methods: {},
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