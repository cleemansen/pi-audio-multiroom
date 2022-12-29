/*
kudos: https://github.com/latovicalmin/vuejs-websockets-example
 */
let webSocketsService = {}

webSocketsService.install = function (Vue, options) {
    let ws = null // new WebSocket(options.url)
    let reconnectInterval = options.reconnectInterval || 1000

    Vue.prototype.$webSocketsConnect = (path, callback) => {
        console.log(window.location.protocol + ' -> ' + options.host)
        ws = new WebSocket(`${options.host}/${path}`)

        ws.onopen = () => {
            options.store.commit('players/wsState', "connecting")
            // Restart reconnect interval
            reconnectInterval = options.reconnectInterval || 1000
        }

        ws.onmessage = (event) => {
            // New message from the backend - use JSON.parse(event.data)
            callback(event)
        }

        ws.onclose = (event) => {
            if (event) {
                // Event.code 1000 is our normal close event
                if (event.code !== 1000) {
                    let maxReconnectInterval = options.maxReconnectInterval || 3000
                    setTimeout(() => {
                        if (reconnectInterval < maxReconnectInterval) {
                            // Reconnect interval can't be > x seconds
                            reconnectInterval += 1000
                        }
                        options.store.commit('players/wsReconnect', event)
                        Vue.prototype.$webSocketsConnect(path, callback)
                    }, reconnectInterval)
                }
            }
        }

        ws.onerror = (error) => {
            console.log(error)
            ws.close()
        }
    }

    Vue.prototype.$webSocketsDisconnect = () => {
        // Our custom disconnect event
        ws.close()
    }

    Vue.prototype.$webSocketsSend = (data) => {
        // Send data to the backend - use JSON.stringify(data)
        ws.send(JSON.stringify(data))
    }
}

export default webSocketsService
