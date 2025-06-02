const { resolve, isMp } = require('./shared')
/** @type {import('tailwindcss').Config} */
module.exports = {
    // 需要tailwindcss提取的文件源，假如你把组件加到了另外的文件，则需要在下方添加表达式
    content: ["./index.html", "./pages/**/*.vue", "./components/**/*.vue"].map(resolve),
    theme: {

    },
    plugins: [],
    corePlugins: {
        preflight: !isMp,
        container: !isMp,
    },
};
