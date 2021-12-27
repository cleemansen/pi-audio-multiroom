<template>
  <div style="background-color: #ececec; padding: 20px">
    <a-row>{{store}}</a-row>
    <a-row :gutter="16" type="flex" justify="center">
      <a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="8" style="background-color: #00acc1">
        <a-page-header
            style="border: 1px solid rgb(235, 237, 240)"
            :ghost="false"
            title="Title"
            sub-title="This is a subtitle">
          <a-row class="content">
            <div style="flex: 1">
              <h1>{{ store.currentTitle }}</h1>
              <p>Hello from the content section!</p>
              <img src="../assets/logo.png" />
            </div>
          </a-row>
        </a-page-header>
      </a-col>
      <a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="8" style="background-color: #00b0ff">
        <a-card :loading="loading" title="2. Card">whatever content</a-card>
        <a-button style="margin-top: 16px" @click="handleClick">Toggle loading</a-button>
      </a-col>
    </a-row>
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

