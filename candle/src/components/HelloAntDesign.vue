<template>
  <a-card :loading="loading" title="Card title">whatever content</a-card>
  <a-button style="margin-top: 16px" @click="handleClick">Toggle loading</a-button>
</template>
<script lang="ts">
import { defineComponent, ref } from 'vue';

export default defineComponent({
  setup() {
    const loading = ref(true);

    const handleClick = () => {
      loading.value = !loading.value;
    };

    return {
      loading,
      handleClick,
    };
  },
});
</script>

