module.exports = {
  "transpileDependencies": [
    "vuetify"
  ],
  devServer: {
    https: true,
    port: 8081,
    proxy: 'https://ktor.pictl.localhost',
    allowedHosts: [
      '.localhost'
    ]
  }
}