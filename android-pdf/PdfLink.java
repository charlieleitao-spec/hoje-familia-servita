package br.org.servitas.familia;

import java.net.URI;
import java.util.Locale;

/** Resolves only the PDF collections embedded by preparar-pdfs-android.py. */
public final class PdfLink {
    public static final class Document {
        public final String path;
        public final int page;
        private Document(String path, int page) { this.path = path; this.page = page; }
    }
    public static Document resolve(String value) {
        try {
            URI uri = new URI(value);
            String host = uri.getHost();
            if (host == null || uri.getUserInfo() != null || uri.getPort() != -1) return null;
            host = host.toLowerCase(Locale.ROOT);
            String scheme = uri.getScheme(), path = uri.getRawPath(), local = null;
            if (!"https".equalsIgnoreCase(scheme)
                    && !("http".equalsIgnoreCase(scheme)
                    && (host.equals("servidimaria.net") || host.equals("www.servidimaria.net")))) return null;
            if (host.equals("charlieleitao-spec.github.io")) {
                if (path.startsWith("/hoje-familia-servita/recursos/pdfs/"))
                    local = path.substring("/hoje-familia-servita/".length());
                else if (path.startsWith("/liturgia-osm/data/pdfs/"))
                    local = path.substring("/liturgia-osm/".length());
                else if (path.startsWith("/liturgia-osm/www/data/pdfs/"))
                    local = path.substring("/liturgia-osm/www/".length());
            } else if (host.equals("raw.githubusercontent.com")
                    && path.startsWith("/charlieleitao-spec/liturgia-osm/main/www/data/pdfs/")) {
                local = path.substring("/charlieleitao-spec/liturgia-osm/main/www/".length());
            } else if (host.equals("servidimaria.net") || host.equals("www.servidimaria.net")) {
                String prefix = "/sitoosm/po/textos-osm/livros-liturgicos-osm/";
                if (path.startsWith(prefix + "missa/"))
                    local = "data/pdfs/missal/" + path.substring((prefix + "missa/").length());
                else if (path.startsWith(prefix + "lhoras/"))
                    local = "data/pdfs/oficios/" + path.substring((prefix + "lhoras/").length());
            }
            if (local == null || !local.matches("(?:data/pdfs/(?:missal|oficios)/[a-z0-9-]+|recursos/pdfs/folha-servita-[a-z0-9-]+)\\.pdf")) return null;
            int page = 1;
            String fragment = uri.getRawFragment();
            if (fragment != null) for (String parameter : fragment.split("&")) {
                if (parameter.matches("page=[0-9]{1,9}")) {
                    int number = Integer.parseInt(parameter.substring(5));
                    if (number > 0) page = number;
                }
            }
            return new Document(local, page);
        } catch (Exception ignored) { return null; }
    }
}
