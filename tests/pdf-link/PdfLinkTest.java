package br.org.servitas.familia;

public final class PdfLinkTest {
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("Falha na resolução do PDF");
    }
    public static void main(String[] args) {
        String[] valid = {
            "https://charlieleitao-spec.github.io/hoje-familia-servita/recursos/pdfs/folha-servita-01-janeiro.pdf",
            "https://charlieleitao-spec.github.io/hoje-familia-servita/recursos/pdfs/folha-servita-ano-completo.pdf",
            "https://raw.githubusercontent.com/charlieleitao-spec/liturgia-osm/main/www/data/pdfs/missal/antonio.pdf#page=3",
            "http://servidimaria.net/sitoosm/po/textos-osm/livros-liturgicos-osm/lhoras/antonio.pdf#page=2"
        };
        for (String value : valid) check(PdfLink.resolve(value) != null);
        check(PdfLink.resolve(valid[2]).page == 3);
        check(PdfLink.resolve(valid[3]).path.equals("data/pdfs/oficios/antonio.pdf"));
        check(PdfLink.resolve(valid[2].replace("page=3", "page=0")).page == 1);
        String[] rejected = {
            "javascript:alert(1)", "file:///data/pdfs/missal/antonio.pdf",
            "https://evil.example/antonio.pdf",
            "https://charlieleitao-spec.github.io.evil.example/hoje-familia-servita/recursos/pdfs/folha-servita-01-janeiro.pdf",
            "https://charlieleitao-spec.github.io/hoje-familia-servita/recursos/pdfs/../private.pdf",
            "https://charlieleitao-spec.github.io/hoje-familia-servita/recursos/pdfs/%2e%2e/private.pdf",
            "https://charlieleitao-spec.github.io/hoje-familia-servita/servitas.ics",
            "https://charlieleitao-spec.github.io/hoje-familia-servita/recursos/folha-servita-pacote.zip",
            "https://liturgiadiaria.edicoescnbb.com.br/"
        };
        for (String value : rejected) check(PdfLink.resolve(value) == null);
        System.out.println("PDFs: caminhos locais, páginas, links oficiais e rejeição de arquivos externos conferidos.");
    }
}
