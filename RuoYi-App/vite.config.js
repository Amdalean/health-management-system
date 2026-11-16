import { defineConfig } from "vite";
import uni from "@dcloudio/vite-plugin-uni";

/**
 * @type {import('vite').UserConfig}
 */
export default defineConfig({
    build: {
    },
    plugins: [
        uni()
    ],
    css: {
        postcss: {
            plugins: [
                require("tailwindcss")({
                    config: require("path").resolve(__dirname, "./tailwind.config.js"),
                }),
                require("autoprefixer")(),
            ],
        },
    },
    server: {
        proxy: {
            '/prod-api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/prod-api/, '')
            }
        }
    },
});