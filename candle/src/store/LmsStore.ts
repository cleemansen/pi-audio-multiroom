import {defineStore} from "pinia";
import {LmsCometDRepository} from "../repo/LmsCometDRepository";
import {Player} from "../types/Player";

export const useLmsStore = defineStore('playerStatus', {
    state: () => ({
        players: [] as Player[],
        currentTitle: "" as string,
        cometD: new LmsCometDRepository(),
    }),
    actions: {
        update(currentState: string) {
            this.currentTitle = currentState
        }
    }
})

