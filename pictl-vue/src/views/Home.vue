<template>
  <div class="home">
    <img alt="Vue logo" src="../assets/logo.png">
    <PlayersOverview />
  </div>
</template>

<script>
// @ is an alias to /src
import PlayersOverview from "@/components/PlayersOverview";

export default {
  name: 'Home',
  components: {
    PlayersOverview
  }
}
</script>
