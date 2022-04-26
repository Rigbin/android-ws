'use strict';

import { v4 } from 'uuid';
import WebSocket, { WebSocketServer } from 'ws';

const wss = new WebSocketServer({
  port: 8080,
  perMessageDeflate: {
    zlibDeflateOptions: {
      chunkSize: 1024,
      memLevel: 7,
      level: 3,
    },
    zlibInflateOptions: {
      chunkSize: 10 * 1024,
    },
    clientNoContextTakeover: true,
    serverNoContextTakeover: true,
    serverMaxWindowBits: 10,
    concurrencyLimit: 10,
    threshold: 1024,
  },
});

let clients = [];

wss.on('connection', (ws) => {
  const client = { ws, id: v4() };
  clients.push(client);
  console.log(`ws connected: ${client.id}`);

  ws.on('message', (data) => {
    console.log(`${client.id}: ${data.toString()}`);
    ws.send(data.toString().split('').reverse().join(''));
  });

  ws.on('close', () => {
    console.log(`ws disconnected: ${client.id}`);
    clients = clients.filter((c) => c.id === client.id);
  });

  ws.on('error', (err) => {
    console.warn(`ws error: ${client.id}: ${err.message}`);
    clients = clients.filter((c) => c.id === client.id);
  });

  ws.send(`Hello ${client.id}`);
});
