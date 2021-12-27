import {CometD} from "cometd";
import {useLmsStore} from "../store/LmsStore";

export class LmsCometDRepository {

    private readonly cometD: CometD
    private connected: boolean = false
    private lmsCometDUrl = "https://lms.unividuell.org" /* "http://localhost:9002" */ + "/cometd"
    private players: []

    constructor() {
        this.cometD = new CometD();
        this.players = []
        // console.debug = console.log;
        this.cometD.unregisterTransport('websocket');

        this.connect()
    }

    connect() {
        this.cometD.addListener("/meta/connect", (message: any) => {
            console.log(`/meta/connect: ` + JSON.stringify(message))
            if (this.connected !== message.successful) {
                this.connected = message.successful;
                console.log(this.connected ? "connected" : "connection failed");
                this.subscribeToRequests()
                this.subscribeToServerStatus()
                this.queryServerStatus()
            }
        });

        console.log("cometD is now initialized")

        this.cometD.configure({
            url: this.lmsCometDUrl,
            useWorkerScheduler: false,
            logLevel: "debug"
        });
        this.cometD.handshake((ack: string) => {
            console.log(`/meta/handshake: ` + JSON.stringify(ack))
        })
    }

    private checkConnected() {
        if (!this.cometD) {
            console.log("Not connected");
            return false;
        }
        if (!this.connected) {
            console.log("Connection failed");
            return false;
        }
        return true;
    }

    private checkPlayer() {
        if (!this.checkConnected()) {
            return false;
        }
        if (this.players?.length < 1) {
            console.log("no players");
            return false;
        }
        return true;
    }

    subscribeToRequests() {
        if (this.checkConnected()) {
            this.cometD.subscribe(
                `/${this.cometD.getClientId()}/slim/request/*`,
                (msg) => console.log(`/slim/request/*: ${JSON.stringify(msg)}`),
                (ack) => console.log(`ACK /slim/request/*: ${JSON.stringify(ack)}`)
            )
        }
    }

    subscribeToServerStatus() {
        if (this.checkConnected()) {
            this.cometD.subscribe(
                `/${this.cometD.getClientId()}/slim/serverstatus`,
                (msg) => {
                    console.log(`/slim/serverstatus: ${JSON.stringify(msg)}`)
                    this.players = msg.data.players_loop.map((player: any) => {
                        return {
                            id: player.playerid,
                            name: player.name
                        }
                    })
                    this.subscribeToPlayerStatus()
                    this.queryPlayerStatus()
                },
                (ack) => console.log(`ACK /slim/serverstatus: ${JSON.stringify(ack)}`)
            )
        }
    }

    queryServerStatus() {
        if (this.checkConnected()) {
            this.cometD.publish(
                "/slim/request",
                {
                    request: ["", ["serverstatus", 0, 255]],
                    response: `/${this.cometD.getClientId()}/slim/serverstatus`
                },
                (ack) => console.log(`ACK /slim/request (serverstatus): ${JSON.stringify(ack)}`)
            )
        }
    }

    subscribeToPlayerStatus() {
        if (this.checkConnected()) {
            this.cometD.subscribe(
                `/${this.cometD.getClientId()}/slim/playerstatus/*`,
                (msg) => {
                    console.log(`/slim/playerstatus/*: ${JSON.stringify(msg)}`)
                    useLmsStore().currentTitle = msg.data.remoteMeta.title
                },
                (ack) => console.log(`ACK /slim/playerstatus/*: ${JSON.stringify(ack)}`)
            )
        }
    }

    queryPlayerStatus() {
        if (this.checkPlayer()) {
            this.players.forEach((player: any) => {
                this.cometD.publish(
                    "/slim/request",
                    {
                        request: [`${player.id}`, ["status", 0, 255, "tags:galKLmNrLT", "subscribe:60"]],
                        response: `/${this.cometD.getClientId()}/slim/playerstatus/${player.id}`
                    },
                    (ack) => console.log(`ACK /slim/request (playerstatus): ${JSON.stringify(ack)}`)
                )
            })
        }
    }

}