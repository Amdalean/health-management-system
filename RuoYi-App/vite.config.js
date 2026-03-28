import { defineConfig } from "vite";
import uni from "@dcloudio/vite-plugin-uni";
import { UnifiedViteWeappTailwindcssPlugin as uvwt } from "weapp-tailwindcss/vite";
const { resolve, WeappTailwindcssDisabled } = require("./shared")

/**
 * @type {import('vite').UserConfig}
 */
export default defineConfig({
    build: {
    },
    plugins: [
        uni(),
        uvwt({
            rem2rpx: true,
            disabled: WeappTailwindcssDisabled,
            tailwindcssBasedir: __dirname
        })
    ],
    css: {
        postcss: {
            plugins: [
                require("tailwindcss")({
                    config: resolve("./tailwind.config.js"),
                }),
                require("autoprefixer")(),
            ],
        },
    },
});