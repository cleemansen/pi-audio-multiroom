module.exports = {
  "transpileDependencies": [
    "vuetify"
  ],
  devServer: {
    port: 8081,
    proxy: {
      '.*': {
        target: 'http://localhost:8080',
        ws: true
      }
    }
  }
}