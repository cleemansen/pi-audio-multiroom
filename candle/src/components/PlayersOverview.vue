<template>
  <div class="layout gutter--md">
    <div class="row">
      <div v-for="player in store.players" class="flex xs12 sm12 md6 lg6 xl6" v-bind:key="player.playerId">
        <va-card>
          <va-app-bar color="secondary">
            <va-button color="#fff" icon="podcasts" flat :rounded="false" :size="16" class="mx-1"/>
            <span style="color: #fff" flat :rounded="false">Candle of {{ store.playerName(player.playerId) }}</span>
          </va-app-bar>
          <va-navbar color="#fff">
            <template #left>
              <va-button icon="menu" color="secondary" />
            </template>
            <template #right>
              <va-button icon="volume_down" color="warning" text-color="primary" />
              <va-button icon="pause" color="secondary" />
              <va-button icon="volume_up" color="secondary" />
            </template>
          </va-navbar>
          <va-image :src="player.artworkUrl" />
          <va-card-title></va-card-title>
          <va-card-content>
            <CurrentTitle :artist="player.artist" :title="player.title" />
          </va-card-content>
        </va-card>
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