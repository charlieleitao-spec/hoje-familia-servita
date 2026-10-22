/* API pública de leitura. Configure BLOGGER_API_KEY nas Propriedades do script. */
const CONFIG = Object.freeze({
  blogUrl: 'https://somosservos.blogspot.com/',
  maxPosts: 20,
  ttl: 300,
  staleTtl: 21600,
  cacheKey: 'somos-servos:v1:posts'
});

function doGet() {
  try { return json_(loadFeed_()); }
  catch (error) {
    // Nunca devolver tokens, chave, URL da API ou mensagens internas ao cliente.
    return json_({schemaVersion: 1, ok: false, error: {
      code: 'FEED_UNAVAILABLE', message: 'Não foi possível atualizar as postagens.'
    }});
  }
}

function json_(value) {
  return ContentService.createTextOutput(JSON.stringify(value))
    .setMimeType(ContentService.MimeType.JSON);
}

function loadFeed_() {
  const cache = CacheService.getScriptCache();
  const cached = cache.get(CONFIG.cacheKey);
  if (cached) return JSON.parse(cached);
  const lock = LockService.getScriptLock();
  if (!lock.tryLock(1000)) {
    const stale = cache.get(CONFIG.cacheKey + ':stale');
    if (stale) return stale_(JSON.parse(stale));
    throw new Error('BUSY');
  }
  try {
    const second = cache.get(CONFIG.cacheKey);
    if (second) return JSON.parse(second);
    const data = fetchFeed_();
    const encoded = JSON.stringify(data);
    // CacheService tem limite de 100 KB por chave; dados nunca incluem corpos HTML.
    if (Utilities.newBlob(encoded).getBytes().length < 95000) {
      cache.put(CONFIG.cacheKey + ':stale', encoded, CONFIG.staleTtl);
      cache.put(CONFIG.cacheKey, encoded, CONFIG.ttl);
    }
    return data;
  } catch (error) {
    const stale = cache.get(CONFIG.cacheKey + ':stale');
    if (stale) return stale_(JSON.parse(stale));
    throw error;
  } finally { lock.releaseLock(); }
}

function stale_(data) {
  data.stale = true; // generatedAt continua sendo o instante da coleta original.
  return data;
}

function api_(path, params, key) {
  const query = Object.keys(params).map(function (name) {
    return encodeURIComponent(name) + '=' + encodeURIComponent(params[name]);
  }).join('&');
  const url = 'https://www.googleapis.com/blogger/v3/' + path + '?' + query
    + '&key=' + encodeURIComponent(key);
  for (let attempt = 0; attempt < 2; attempt++) {
    const response = UrlFetchApp.fetch(url, {muteHttpExceptions: true});
    const status = response.getResponseCode();
    if (status === 200) return JSON.parse(response.getContentText());
    if (attempt === 0 && (status === 429 || status >= 500)) {
      Utilities.sleep(400 + Math.floor(Math.random() * 200));
      continue;
    }
    // Somente código HTTP e motivo estruturado; nunca chave, URL ou mensagem da API.
    let reason = '';
    try {
      const body = JSON.parse(response.getContentText());
      const failure = body.error || {};
      const detail = (failure.details || []).find(function (item) { return item.reason; });
      const candidate = /API key not valid/i.test(String(failure.message || '')) ? 'API_KEY_INVALID'
        : (detail && detail.reason) || (failure.errors && failure.errors[0] && failure.errors[0].reason);
      if (typeof candidate === 'string' && /^[A-Za-z_]{1,80}$/.test(candidate)) reason = candidate;
    } catch (_) {}
    throw new Error('BLOGGER_REQUEST_FAILED HTTP ' + status + (reason ? ' ' + reason : ''));
  }
}

function fetchFeed_() {
  const props = PropertiesService.getScriptProperties();
  const key = props.getProperty('BLOGGER_API_KEY');
  if (!key) throw new Error('CONFIG_REQUIRED');
  let blogId = props.getProperty('BLOGGER_BLOG_ID');
  if (!blogId) {
    blogId = api_('blogs/byurl', {url: CONFIG.blogUrl, fields: 'id'}, key).id;
    if (!/^\d+$/.test(String(blogId))) throw new Error('INVALID_BLOG_ID');
    props.setProperty('BLOGGER_BLOG_ID', String(blogId));
  }
  if (!/^\d+$/.test(blogId)) throw new Error('INVALID_BLOG_ID');
  const result = api_('blogs/' + blogId + '/posts', {
    maxResults: CONFIG.maxPosts, orderBy: 'published', status: 'live', view: 'READER',
    fetchBodies: false, fetchImages: true,
    fields: 'items(id,title,url,published,images(url))'
  }, key);
  return {
    schemaVersion: 1, ok: true, stale: false, generatedAt: new Date().toISOString(),
    blog: {title: 'Somos Servos', url: CONFIG.blogUrl},
    posts: (result.items || []).map(function (post) {
      return {id: String(post.id), title: String(post.title || '').slice(0, 500),
        url: https_(post.url), published: post.published,
        thumbnail: https_(post.images && post.images[0] && post.images[0].url)};
    }).filter(function (post) { return post.url && !isNaN(Date.parse(post.published)); }),
    materials: materials_()
  };
}

function https_(value) {
  if (typeof value !== 'string' || value.length > 4096) return null;
  value = value.replace(/^http:\/\//i, 'https://');
  return /^https:\/\/[^\s/]+(?:\/[^\s]*)?$/i.test(value) ? value : null;
}

function materials_() {
  const base = 'https://charlieleitao-spec.github.io/hoje-familia-servita/';
  return [
    {title: 'Hoje na Família Servita', url: base, type: 'web'},
    {title: 'Folhas do ano (PDF)', url: base + 'recursos/pdfs/folha-servita-ano-completo.pdf', type: 'pdf'},
    {title: 'Pacote completo', url: base + 'recursos/folha-servita-pacote.zip', type: 'zip'},
    {title: 'Calendário servita', url: base + 'servitas.ics', type: 'ics'}
  ];
}

/* Execute uma vez no editor: autoriza UrlFetch e verifica a configuração. */
function testarConfiguracao() {
  const data = fetchFeed_();
  console.log('Configuração válida. Postagens: ' + data.posts.length);
}
