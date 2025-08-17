import pluginVue from "eslint-plugin-vue";
import { defineConfigWithVueTs, vueTsConfigs } from "@vue/eslint-config-typescript";
import skipFormatting from "@vue/eslint-config-prettier/skip-formatting";

// kudos: https://vueschool.io/articles/vuejs-tutorials/upgrading-eslint-from-v8-to-v9-in-vue-js/
// kudos: https://eslint.org/blog/2022/08/new-config-system-part-2/
export default [
  {
    name: "app/files-to-lint",
    files: ["**/*.{ts,mts,tsx,vue}"],
  },
  {
    name: "app/files-to-ignore",
    ignores: ["**/dist/**", "**/dist-ssr/**", "**/coverage/**", "pnpm-lock.yaml"],
  },
  ...pluginVue.configs["flat/recommended"],
  {
    rules: {
      "vue/multi-word-component-names": 0,
      // https://github.com/vuetifyjs/vuetify/issues/20487#issuecomment-2357820535
      "vue/valid-v-slot": [
        "error",
        {
          allowModifiers: true,
        },
      ],
    },
  },
  // ...vueTsEslintConfig(), <- deprecated, is the next line the same?
  ...defineConfigWithVueTs(vueTsConfigs.recommended),
  skipFormatting,
];
