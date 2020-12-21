import Vue from "vue";

Vue.mixin({
    methods: {
        // there is not native `map`-function for objects
        // kudos: https://stackoverflow.com/a/14810722/810944
        objectMap: (obj, fn) =>
            Object.fromEntries(
                Object.entries(obj).map(
                    ([k, v], i) => [k, fn(v, k, i)]
                )
            )
    }
})