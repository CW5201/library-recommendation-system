import { mkdirSync } from 'node:fs'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const root = fileURLToPath(new URL('..', import.meta.url))
const tmp = fileURLToPath(new URL('../.vite-tmp', import.meta.url))
const vite = fileURLToPath(new URL('../node_modules/vite/bin/vite.js', import.meta.url))

mkdirSync(tmp, { recursive: true })

const args = process.argv.slice(2)
const result = spawnSync(process.execPath, [vite, ...(args.length ? args : ['build'])], {
  cwd: root,
  stdio: 'inherit',
  env: {
    ...process.env,
    TMP: tmp,
    TEMP: tmp
  }
})

process.exit(result.status === null ? 1 : result.status)
