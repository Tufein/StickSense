// Opslag in IndexedDB: bestanden (saves/configs) en profielen. Alles blijft lokaal.
const DB = 'sticksense';

function open() {
  return new Promise((res, rej) => {
    const r = indexedDB.open(DB, 1);
    r.onupgradeneeded = () => {
      r.result.createObjectStore('files', { keyPath: 'name' });
      r.result.createObjectStore('profiles', { keyPath: 'id' });
    };
    r.onsuccess = () => res(r.result);
    r.onerror = () => rej(r.error);
  });
}

async function tx(store, mode, fn) {
  const db = await open();
  return new Promise((res, rej) => {
    const t = db.transaction(store, mode);
    const out = fn(t.objectStore(store));
    t.oncomplete = () => { db.close(); res(out && 'result' in out ? out.result : undefined); };
    t.onerror = () => rej(t.error);
  });
}

export const getAll = (store) => tx(store, 'readonly', (s) => s.getAll());
export const put = (store, v) => tx(store, 'readwrite', (s) => s.put(v));
export const del = (store, k) => tx(store, 'readwrite', (s) => s.delete(k));
export const clear = (store) => tx(store, 'readwrite', (s) => s.clear());
