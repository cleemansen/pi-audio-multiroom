<template>
  <div>
    <p class="display-1 text--primary">{{ currentSong }}</p>
  </div>
</template>

<script>
export default {
  name: "CurrentTitle",
  props: ['artist', 'title'],
  data: () => ({}),
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
  }
}
</script>

<style scoped>

</style>