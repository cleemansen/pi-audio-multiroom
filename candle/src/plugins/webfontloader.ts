/**
 * plugins/webfontloader.js
 *
 * webfontloader documentation: https://github.com/typekit/webfontloader
 * @async can run in the background
 */
export async function loadFonts() {
  const webFontLoader = await import(
    /* webpackChunkName: "webfontloader" */ "webfontloader"
  );

  webFontLoader.load({
    google: {
      families: ["Roboto:100,300,400,500,700,900&display=swap"],
    },
  });
}
