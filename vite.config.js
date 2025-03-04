import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
    plugins: [react()],
    build: {
        sourcemap: true,
        rollupOptions: {
            input: {
                App: 'src/jsx/App.jsx',
            },
            output: {
                dir: 'src/main/webapp/resources',
                entryFileNames: 'script/[name].js',
                chunkFileNames: 'script/[name].js',
                assetFileNames: '[ext]/[name].[ext]',
                manualChunks: (id) => id.includes('node_modules') ? 'vendor' : null
            },
        },
    },
})
