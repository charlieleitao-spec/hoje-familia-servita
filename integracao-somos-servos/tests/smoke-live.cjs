// Verificação real sem cookies nem chave; não substitui o teste no WebView.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

async function main() {
  const html = fs.readFileSync(path.join(__dirname, '../../somos-servos.html'), 'utf8');
  let url = html.match(/const API_URL = '([^']+)';/)?.[1];
  assert.match(url || '', /^https:\/\/script\.google\.com\/macros\/s\/[A-Za-z0-9_-]+\/exec$/);
  const origin = 'https://charlieleitao-spec.github.io';
  for (let hop = 0; hop < 5; hop++) {
    const response = await fetch(url, {
      headers: {Origin: origin}, redirect: 'manual', credentials: 'omit',
      signal: AbortSignal.timeout(30000)
    });
    const cors = response.headers.get('access-control-allow-origin');
    assert.ok(cors === '*' || cors === origin, `CORS ausente no salto ${hop}: HTTP ${response.status}`);
    if ([301, 302, 303, 307, 308].includes(response.status)) {
      const next = new URL(response.headers.get('location'), url);
      assert.equal(next.protocol, 'https:');
      assert.ok(['script.google.com', 'script.googleusercontent.com'].includes(next.hostname));
      url = next.href;
      await response.body?.cancel();
      continue;
    }
    assert.equal(response.status, 200);
    assert.match(response.headers.get('content-type') || '', /application\/json/);
    const feed = await response.json();
    assert.equal(feed.ok, true);
    assert.equal(feed.schemaVersion, 1);
    assert.equal(typeof feed.stale, 'boolean');
    assert.ok(Number.isFinite(Date.parse(feed.generatedAt)));
    assert.ok(Array.isArray(feed.posts) && feed.posts.length <= 20);
    for (const post of feed.posts) {
      assert.equal(typeof post.title, 'string');
      assert.equal(new URL(post.url).hostname, 'somosservos.blogspot.com');
      assert.ok(Number.isFinite(Date.parse(post.published)));
      assert.ok(post.thumbnail === null || new URL(post.thumbnail).protocol === 'https:');
    }
    assert.ok(Array.isArray(feed.materials) && feed.materials.length === 4);
    console.log(`Implantação anônima OK: ${feed.posts.length} postagens; JSON e cabeçalhos CORS válidos.`);
    return;
  }
  throw new Error('Limite de redirecionamentos excedido');
}

main().catch(error => {
  // Não imprimir respostas da API nem URLs transitórias de redirecionamento.
  console.error('Teste da implantação falhou:', error.message);
  process.exitCode = 1;
});
