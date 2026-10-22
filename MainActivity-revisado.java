package br.org.servitas.familia;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.Locale;

/**
 * Família Servita 2.9.31 (versionCode 312)
 *
 * Correção: links para arquivos (PDF das folhas mensais, "folhas do ano",
 * "pacote completo", .ics) ficavam presos dentro do WebView, que não exibe
 * PDF e não tinha DownloadListener — o toque não fazia nada. Agora esses
 * links (e qualquer download) abrem fora do app, no navegador/visualizador.
 */
public class MainActivity extends Activity {

    private static final String HOST = "charlieleitao-spec.github.io";
    private static final String APP_PATH = "/hoje-familia-servita/";
    private static final String HOME = "https://" + HOST + APP_PATH;
    private static final String OFFLINE = HOME + "somos-servos.html";

    private static final String[] FILE_EXTENSIONS = {
            ".pdf", ".ics", ".zip", ".docx", ".doc", ".xlsx", ".csv"
    };

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(23, 59, 103));
        getWindow().setNavigationBarColor(Color.rgb(247, 244, 237));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(247, 244, 237));
        webView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setDatabaseEnabled(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);

        webView.setWebViewClient(new WebViewClient() {
            // A mesma URL HTTPS serve o asset local online e offline.
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("https".equalsIgnoreCase(uri.getScheme()) && HOST.equalsIgnoreCase(uri.getHost())
                        && (APP_PATH + "somos-servos.html").equals(uri.getPath())) {
                    try {
                        return new WebResourceResponse("text/html", "UTF-8",
                                getAssets().open("somos-servos.html"));
                    } catch (IOException error) {
                        // Resposta finita: não iniciar um ciclo de fallback se faltar o asset.
                        return new WebResourceResponse("text/plain", "UTF-8", 500, "Missing asset",
                                java.util.Collections.emptyMap(), new ByteArrayInputStream(
                                "Pagina local indisponivel.".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                    }
                }
                return null;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                String path = uri.getPath();

                boolean inApp = "https".equals(scheme)
                        && HOST.equals(uri.getHost())
                        && path != null
                        && (path.startsWith(APP_PATH) || path.equals(APP_PATH.substring(0, APP_PATH.length() - 1)))
                        && !isFileLink(path);
                if (inApp) {
                    return false; // navegação normal dentro do app
                }

                if (isAllowedExternalScheme(uri)) {
                    openExternal(uri);
                }
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame() && isInternalPage(request.getUrl())) {
                    view.loadUrl(OFFLINE);
                }
            }
        });

        // Captura links target="_blank" e window.open sem deixar uma janela vazia.
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog,
                                          boolean isUserGesture, Message resultMsg) {
                if (!isUserGesture || resultMsg == null) return false;
                final WebView popup = new WebView(MainActivity.this);
                popup.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView child, WebResourceRequest request) {
                        Uri uri = request.getUrl();
                        if (isAllowedExternalScheme(uri)) openExternal(uri);
                        child.post(child::destroy);
                        return true;
                    }
                });
                WebView.WebViewTransport transport =
                        (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(popup);
                resultMsg.sendToTarget();
                return true;
            }
        });

        // Qualquer resposta que o WebView trate como download (PDF, .ics, zip…)
        // é entregue ao navegador, em vez de ser descartada em silêncio.
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) ->
                openExternal(Uri.parse(url)));

        if (savedInstanceState == null) {
            webView.loadUrl(HOME);
        } else if (webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl(HOME);
        }
    }

    private static boolean isFileLink(String path) {
        String p = path.toLowerCase(Locale.ROOT);
        for (String ext : FILE_EXTENSIONS) {
            if (p.endsWith(ext)) return true;
        }
        return false;
    }

    private static boolean isInternalPage(Uri uri) {
        String path = uri.getPath();
        return "https".equalsIgnoreCase(uri.getScheme())
                && HOST.equalsIgnoreCase(uri.getHost())
                && path != null && (path.startsWith(APP_PATH)
                || path.equals(APP_PATH.substring(0, APP_PATH.length() - 1)));
    }

    private static boolean isAllowedExternalScheme(Uri uri) {
        String scheme = uri.getScheme();
        return "https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)
                || "mailto".equalsIgnoreCase(scheme) || "tel".equalsIgnoreCase(scheme);
    }

    private void openExternal(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "Nenhum aplicativo disponível para abrir este link.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
