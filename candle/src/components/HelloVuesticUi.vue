<template>
  <div style="background-color: #00b0ff">
    <div class="layout gutter--md">
      <div class="row">
        <div class="flex xs12 sm12 md6 lg6 xl6">
          <va-card>
            <va-card-title>Card 0</va-card-title>
            <va-card-content>{{ store.currentTitle }}</va-card-content>
          </va-card>
        </div>
        <div class="flex xs12 sm12 md6 lg6 xl6">
          <va-card>
            <va-card-title>Card 1</va-card-title>
            <va-card-content>{{ store.currentTitle }}</va-card-content>
          </va-card>
        </div>
      </div>
    </div>
  </div>
</template>
<script lang="ts">
import {defineComponent, ref} from 'vue';
import {useLmsStore} from '../store/LmsStore'

export default defineComponent({
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

