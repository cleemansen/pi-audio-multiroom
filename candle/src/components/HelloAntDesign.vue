<template>
  <div style="background-color: #ececec; padding: 20px">

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

