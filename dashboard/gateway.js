/**
 * Lightweight WebSocket client for the OpenClaw gateway.
 * Connects to ws://127.0.0.1:18789 (default) and exposes
 * typed request methods for agents, sessions, and usage.
 */

class GatewayClient {
  constructor(url = "ws://127.0.0.1:18789") {
    this.url = url;
    this.ws = null;
    this.connected = false;
    this.pending = new Map();
    this.seq = 0;
    this._onStatusChange = null;
  }

  /** Register a callback for connection status changes */
  onStatusChange(fn) { this._onStatusChange = fn; }

  _setStatus(connected) {
    this.connected = connected;
    if (this._onStatusChange) this._onStatusChange(connected);
  }

  _id() { return `dj-${++this.seq}-${Date.now()}`; }

  /**
   * Connect to the gateway. Resolves with hello-ok payload or rejects.
   * Handles the optional challenge-nonce flow.
   */
  connect() {
    return new Promise((resolve, reject) => {
      try {
        this.ws = new WebSocket(this.url);
      } catch (err) {
        reject(err);
        return;
      }

      const timeout = setTimeout(() => {
        reject(new Error("Gateway connection timeout (10s)"));
        this.ws?.close();
      }, 10_000);

      let challengeNonce = null;
      let connectSent = false;

      const sendConnect = () => {
        if (connectSent) return;
        connectSent = true;

        const id = this._id();
        const frame = {
          type: "req",
          id,
          method: "connect",
          params: {
            minProtocol: 1,
            maxProtocol: 3,
            client: {
              id: "dejima-dashboard",
              displayName: "New Dejima Dashboard",
              version: "1.0.0",
              platform: navigator?.platform ?? "web",
              mode: "control",
            },
            caps: [],
            ...(challengeNonce ? { nonce: challengeNonce } : {}),
          },
        };
        this.ws.send(JSON.stringify(frame));
        this.pending.set(id, { resolve: (payload) => {
          clearTimeout(timeout);
          this._setStatus(true);
          resolve(payload);
        }, reject: (err) => {
          clearTimeout(timeout);
          reject(err);
        }});
      };

      this.ws.addEventListener("open", () => {
        setTimeout(sendConnect, 800);
      });

      this.ws.addEventListener("message", (ev) => {
        let msg;
        try { msg = JSON.parse(ev.data); } catch { return; }

        if (msg.type === "event" && msg.event === "connect.challenge") {
          challengeNonce = msg.payload?.nonce ?? null;
          sendConnect();
          return;
        }

        if (msg.type === "res") {
          const p = this.pending.get(msg.id);
          if (p) {
            this.pending.delete(msg.id);
            if (msg.ok) p.resolve(msg.payload);
            else p.reject(new Error(msg.error?.message ?? "Request failed"));
          }
        }
      });

      this.ws.addEventListener("close", () => {
        this._setStatus(false);
        for (const [, p] of this.pending) {
          p.reject(new Error("Connection closed"));
        }
        this.pending.clear();
      });

      this.ws.addEventListener("error", () => {
        // close handler covers cleanup
      });
    });
  }

  /** Send a request and return the response payload. */
  request(method, params = {}) {
    return new Promise((resolve, reject) => {
      if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
        reject(new Error("Not connected"));
        return;
      }
      const id = this._id();
      this.pending.set(id, { resolve, reject });
      this.ws.send(JSON.stringify({ type: "req", id, method, params }));
      setTimeout(() => {
        if (this.pending.has(id)) {
          this.pending.delete(id);
          reject(new Error(`Timeout: ${method}`));
        }
      }, 15_000);
    });
  }

  disconnect() {
    this.ws?.close();
    this.ws = null;
    this._setStatus(false);
  }

  /* ── Convenience methods ──────────────────────────── */

  async listAgents() {
    return this.request("agents.list", {});
  }

  async getUsage(opts = {}) {
    const params = {
      days: opts.days ?? 30,
      limit: opts.limit ?? 500,
      ...opts,
    };
    return this.request("sessions.usage", params);
  }

  async getCost(opts = {}) {
    const params = {
      days: opts.days ?? 30,
      ...opts,
    };
    return this.request("usage.cost", params);
  }

  async listSessions(opts = {}) {
    return this.request("sessions.list", {
      limit: opts.limit ?? 100,
      includeGlobal: true,
      includeUnknown: true,
      ...opts,
    });
  }

  async getStatus() {
    return this.request("status", {});
  }
}
