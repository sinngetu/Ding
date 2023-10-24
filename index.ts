import net from 'node:net'
import { exec } from 'node:child_process'
import Debug from 'debug'
import notifier from 'node-notifier'

import config from './config.json' assert { type: 'json' }

const debug = Debug('subscribe-client')
const socket = (() => {
    try { return net.connect(config.port, config.host, () => debug('server connected!')) }
    catch(e) {
        notifier.notify({ title: '连接失败！！请重试！' })
        process.exit(0)
    }
})()

interface New {
    link: string
    hash: string
    title: string
    medium: number
    date: string
    tags: string
    status: number
    keyword: string
}

notifier.notify({ title: '通知系统连接成功 :)' })

socket.setKeepAlive(true)
socket.on('data', chunk => {
    const news: New = JSON.parse(chunk.toString())

    notifier.notify({
        title: getMedium(news.medium),
        message: news.title
    }, (err, response) => {
        // if (response === 'activate')
        exec(`start ${news.link}`)
    })
})

socket.on('error', () => {
    notifier.notify({
        title: '通知系统挂了！快重启！！',
        message: '系统恢复后重启来链接',
        time: 0
    })

    process.exit(0)
})

function getMedium(medium: number) {
    switch (medium) {
        case 1:  return '华尔街日报'
        case 22: return '金融时报中文网'
        case 23: return '金融时报'
        case 24: return '路透社'
        case 25: return '路透社'
        case 26: return '彭博社'
        case 27: return '彭博社'
        case 64: return '华尔街日报中文网'
        default: return 'BigNews'
    }
}
