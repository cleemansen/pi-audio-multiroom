<template>
  <div class="container">
    <div class="row">
      <div v-for="player in store.players" class="col-12 col-sm-12 	col-md-6 col-lg-6 col-xl-6 col-xxl-6" v-bind:key="player.playerId">
        <div class="card shadow rounded">
          <div class="card-header">{{ store.playerName(player.playerId) }}</div>
          <img :src="player.artworkUrl"/>
          <div class="card-body">
            <CurrentTitle :artist="player.artist" :title="player.title"/>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script lang="ts">
import {defineComponent, ref} from 'vue';
import {useLmsStore} from '../store/LmsStore'
import CurrentTitle from "./CurrentTitle.vue";

export default defineComponent({
  components: {CurrentTitle},
  setup() {
    const loading = ref(true);
    const store = useLmsStore()

    // store.init()

    const handleClick = () => {
      loading.value = !loading.value;
    };

    return {
      loading,
      handleClick,
      store
    };
  },
});
</script>

<style>
:root {
  --va-app-bar-height: 1.2rem
}
</style>