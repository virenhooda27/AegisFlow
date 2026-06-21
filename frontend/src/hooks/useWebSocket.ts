import { useEffect, useRef, useCallback, useState } from 'react';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WS_URL = '/ws';

let sharedClient: Client | null = null;
let refCount = 0;

function getOrCreateClient(): Client {
  if (!sharedClient) {
    sharedClient = new Client({
      webSocketFactory: () => new SockJS(WS_URL) as unknown as WebSocket,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });
    sharedClient.activate();
  }
  refCount++;
  return sharedClient;
}

function releaseClient() {
  refCount--;
  if (refCount <= 0 && sharedClient) {
    sharedClient.deactivate();
    sharedClient = null;
    refCount = 0;
  }
}

export function useStompSubscription<T>(
  topic: string,
  onMessage: (data: T) => void,
  enabled = true
) {
  const clientRef = useRef<Client | null>(null);
  const callbackRef = useRef(onMessage);
  callbackRef.current = onMessage;

  useEffect(() => {
    if (!enabled) return;

    const client = getOrCreateClient();
    clientRef.current = client;

    const subscribe = () => {
      if (client.connected) {
        const sub = client.subscribe(topic, (message: IMessage) => {
          try {
            const data = JSON.parse(message.body) as T;
            callbackRef.current(data);
          } catch {
            // ignore parse errors
          }
        });
        return sub;
      }
      return null;
    };

    let sub = subscribe();

    const onConnect = client.onConnect;
    client.onConnect = (frame) => {
      onConnect?.(frame);
      sub = subscribe();
    };

    return () => {
      sub?.unsubscribe();
      releaseClient();
    };
  }, [topic, enabled]);
}

export function useConnectionStatus(): boolean {
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    const client = getOrCreateClient();

    const checkConnection = () => setConnected(client.connected);
    const interval = setInterval(checkConnection, 1000);
    checkConnection();

    return () => {
      clearInterval(interval);
      releaseClient();
    };
  }, []);

  return connected;
}
