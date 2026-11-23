import { Client } from '@stomp/stompjs';

/**
 * Creates a STOMP WebSocket client with automatic reconnection (Point 5).
 * Uses wss:// in production via NEXT_PUBLIC_WS_URL.
 */
export function createStompClient({ token, onConnect, onDisconnect, onError }) {
  const wsUrl = process.env.NEXT_PUBLIC_WS_URL || 'ws://localhost:8080/ws-native';

  const client = new Client({
    brokerURL: wsUrl,
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    reconnectDelay: 3000, // Auto-reconnect after a network drop
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      if (onConnect) onConnect(client);
    },
    onWebSocketClose: () => {
      if (onDisconnect) onDisconnect();
    },
    onStompError: (frame) => {
      if (onError) onError(frame.headers?.message || 'WebSocket error');
    },
  });

  client.activate();
  return client;
}
