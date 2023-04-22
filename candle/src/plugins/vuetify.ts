// Styles
import "@mdi/font/css/materialdesignicons.css";
import "vuetify/styles";

// Vuetify
import { createVuetify, ThemeDefinition } from "vuetify";

// kudos: https://next.vuetifyjs.com/en/features/theme/#changing-theme
const lightTheme: ThemeDefinition = {
  dark: false,
  colors: {
    primary: "#000000",
  },
};
const darkTheme: ThemeDefinition = {
  dark: true,
  colors: {
    primary: "#ffffff",
  },
};

const vuetify = createVuetify({
  theme: {
    defaultTheme:
      window.matchMedia &&
      window.matchMedia("(prefers-color-scheme: light)").matches
        ? "lightTheme"
        : "darkTheme",
    themes: {
      lightTheme,
      darkTheme,
    },
  },
});

export default vuetify;
