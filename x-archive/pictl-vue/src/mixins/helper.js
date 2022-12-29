import Vue from "vue";

const Mixin = {
    methods: {
        // there is no native `map`-function for objects
        // kudos: https://stackoverflow.com/a/14810722/810944
        objectMap: (obj, fn) =>
            Object.fromEntries(
                Object.entries(obj).map(
                    ([k, v], i) => [k, fn(v, k, i)]
                )
            ),
        // kudos: https://stackoverflow.com/a/37616104/810944
        objectFilter: (obj, predicate) =>
            Object.fromEntries(Object.entries(obj).filter(predicate))
    }
}

// needed for access outside of components (eg vuex)
export {Mixin}

Vue.mixin(Mixin)