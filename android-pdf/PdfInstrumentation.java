package br.org.servitas.familia.validation;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.view.View;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import br.org.servitas.familia.LocalPdfActivity;
import br.org.servitas.familia.MainActivity;
import android.webkit.WebView;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import br.org.servitas.familia.LocalPdfStore;
import br.org.servitas.familia.LocalPdfPrintAdapter;

/** Runs against the signed release, using the Android renderer and print service. */
public final class PdfInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            Context context = getTargetContext();
            int files = 0, pages = 0;
            for (String directory : new String[]{"data/pdfs/missal", "data/pdfs/oficios", "data/pdfs/biblioteca", "recursos/pdfs"}) {
                for (String name : context.getAssets().list("public/" + directory)) {
                    if (!name.endsWith(".pdf")) continue;
                    String path = directory + "/" + name;
                    ByteArrayOutputStream printed = new ByteArrayOutputStream();
                    LocalPdfPrintAdapter.writeOriginal(context, path, printed, new CancellationSignal());
                    try (InputStream input = context.getAssets().open("public/" + path)) {
                        if (!Arrays.equals(input.readAllBytes(), printed.toByteArray())) throw new Exception("Print changed " + path);
                    }
                    try (PdfRenderer renderer = LocalPdfStore.open(context, path)) {
                        files++;
                        for (int index = 0; index < renderer.getPageCount(); index++) {
                            try (PdfRenderer.Page page = renderer.openPage(index)) {
                                Bitmap bitmap = Bitmap.createBitmap(96, Math.max(1, 96 * page.getHeight() / page.getWidth()), Bitmap.Config.ARGB_8888);
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                                bitmap.recycle(); pages++;
                            }
                        }
                    }
                }
            }
            if (files != 79 || pages != 618) throw new Exception("PDF inventory " + files + "/" + pages);
            Activity viewer = startActivitySync(new Intent(context, LocalPdfActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("path", "recursos/pdfs/folha-servita-ano-completo.pdf")
                .putExtra("page", 2).putExtra("title", "Folhas do ano"));
            waitForIdleSync();
            boolean focused = false;
            for (int attempt = 0; attempt < 80; attempt++) {
                java.util.concurrent.atomic.AtomicBoolean hasFocus = new java.util.concurrent.atomic.AtomicBoolean();
                runOnMainSync(() -> hasFocus.set(viewer.hasWindowFocus()));
                if (hasFocus.get()) { focused = true; break; }
                Thread.sleep(250);
            }
            if (!focused) throw new Exception("PDF viewer did not gain window focus");
            runOnMainSync(() -> {
                View root = viewer.getWindow().getDecorView();
                if (!((android.widget.TextView) root.findViewWithTag("pdf-position")).getText().toString().startsWith("Página 2 de")) throw new AssertionError("Initial page");
                root.findViewWithTag("pdf-zoom-in").performClick();
                if (!root.findViewWithTag("pdf-page").getContentDescription().toString().contains("125%")) throw new AssertionError("Zoom");
                root.findViewWithTag("pdf-print").performClick();
            });
            boolean opened = false;
            for (int attempt = 0; attempt < 240; attempt++) {
                android.view.accessibility.AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
                if (root != null && "com.android.printspooler".equals(String.valueOf(root.getPackageName()))) { opened = true; break; }
                Thread.sleep(250);
            }
            if (!opened) {
                android.view.accessibility.AccessibilityNodeInfo window = getUiAutomation().getRootInActiveWindow();
                throw new Exception("Print UI did not open; active window: " + (window == null ? "none" : window.getPackageName() + " / " + window.getText()));
            }
            getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
            boolean returned = false;
            for (int attempt = 0; attempt < 60; attempt++) {
                android.view.accessibility.AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
                if (root != null && context.getPackageName().equals(String.valueOf(root.getPackageName()))) { returned = true; break; }
                Thread.sleep(250);
            }
            if (!returned) throw new Exception("Print cancel did not return");
            runOnMainSync(() -> {
                View root = viewer.getWindow().getDecorView();
                if (!((android.widget.TextView) root.findViewWithTag("pdf-position")).getText().toString().startsWith("Página 2 de")) throw new AssertionError("Page after print");
                if (!root.findViewWithTag("pdf-page").getContentDescription().toString().contains("125%")) throw new AssertionError("Zoom after print");
                root.findViewWithTag("pdf-next").performClick();
                if (!((android.widget.TextView) root.findViewWithTag("pdf-position")).getText().toString().startsWith("Página 3 de")) throw new AssertionError("Next page");
                root.findViewWithTag("pdf-close").performClick();
            });
            verifyOfflineLibraryLinks(context);
            Activity library = startActivitySync(new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            java.lang.reflect.Field field = MainActivity.class.getDeclaredField("webView");
            field.setAccessible(true);
            WebView web = (WebView) field.get(library);
            runOnMainSync(() -> web.loadUrl("https://charlieleitao-spec.github.io/hoje-familia-servita/biblioteca-servita.html"));
            boolean catalogLoaded = false;
            for (int attempt = 0; attempt < 80; attempt++) {
                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<String> value = new AtomicReference<>("");
                runOnMainSync(() -> web.evaluateJavascript("document.querySelectorAll('#documents article').length",
                    resultValue -> { value.set(resultValue); latch.countDown(); }));
                latch.await(2, TimeUnit.SECONDS);
                if ("5".equals(value.get())) { catalogLoaded = true; break; }
                Thread.sleep(250);
            }
            if (!catalogLoaded) throw new Exception("Library catalogue did not open offline");
            runOnMainSync(library::finish);
            for (int initialPage : new int[]{43, 25, 131}) {
                Activity direct = startActivitySync(new Intent(context, LocalPdfActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra("path", "data/pdfs/biblioteca/constituicoes-regra-diretorio-osm-2015.pdf").putExtra("page", initialPage));
                waitForIdleSync();
                runOnMainSync(() -> {
                    android.widget.TextView position = direct.getWindow().getDecorView().findViewWithTag("pdf-position");
                    if (!position.getText().toString().startsWith("Página " + initialPage + " de")) throw new AssertionError("Library page " + initialPage);
                    direct.finish();
                });
                waitForIdleSync();
            }
            verifyRotation(context);
            result.putString("validation", "PASS");
            result.putString("stream", "PASS: " + files + " PDFs; " + pages + " pages; original print bytes; zoom; Android printing; cancel and return.\nPASS: PDF rotation retains page 44 and zoom 125% in landscape and portrait.\nPASS: Fresh offline fallback opens Library links at pages 43, 25 and 131.\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("validation", "FAIL");
            result.putString("stream", android.util.Log.getStackTraceString(error));
            finish(Activity.RESULT_CANCELED, result);
        }
    }
    private boolean webIs(WebView web,String expression) throws Exception {
        CountDownLatch latch=new CountDownLatch(1);
        AtomicReference<String> value=new AtomicReference<>("");
        runOnMainSync(() -> web.evaluateJavascript(expression,resultValue -> {value.set(resultValue);latch.countDown();}));
        if(!latch.await(3,TimeUnit.SECONDS))throw new Exception("WebView callback timeout");
        return "true".equals(value.get());
    }
    private void waitForWeb(WebView web,String expression) throws Exception {
        for(int attempt=0;attempt<80;attempt++){
            if(webIs(web,expression))return;
            Thread.sleep(250);
        }
        CountDownLatch latch=new CountDownLatch(1);
        AtomicReference<String> details=new AtomicReference<>("");
        runOnMainSync(() -> web.evaluateJavascript("JSON.stringify({url:location.href,title:document.title,body:document.body?document.body.innerText.slice(0,1200):''})",value->{details.set(value);latch.countDown();}));
        latch.await(3,TimeUnit.SECONDS);
        throw new Exception("Offline WebView condition failed: "+expression+"; state="+details.get());
    }
    private void verifyOfflineLibraryLinks(Context context) throws Exception {
        Activity main=startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            java.lang.reflect.Field field=MainActivity.class.getDeclaredField("webView");
            field.setAccessible(true);
            WebView web=(WebView)field.get(main);
            // This is the first MainActivity in a fresh debug installation without a network.
            // Its onCreate loads HOME once; avoid a second request racing the fallback.
            waitForWeb(web,"!!document.getElementById('offline-title')");
            runOnMainSync(() -> web.evaluateJavascript("document.querySelector('a[href$=\"biblioteca-servita.html\"]').click()",null));
            waitForWeb(web,"document.querySelectorAll('#documents article').length===5");
            int[] pages={43,25,131};
            for(int index=0;index<pages.length;index++){
                final int selected=index,expectedPage=pages[index];
                ActivityMonitor monitor=addMonitor(LocalPdfActivity.class.getName(),null,false);
                Activity reader=null;
                try {
                    runOnMainSync(() -> web.evaluateJavascript("document.querySelectorAll('#documents article a')["+selected+"].click()",null));
                    reader=waitForMonitorWithTimeout(monitor,15000);
                    if(reader==null)throw new Exception("Library PDF link did not open native viewer");
                    waitForIdleSync();
                    final Activity opened=reader;
                    runOnMainSync(() -> {
                        android.widget.TextView position=opened.getWindow().getDecorView().findViewWithTag("pdf-position");
                        if(!position.getText().toString().startsWith("Página "+expectedPage+" de"))throw new AssertionError("Library link page "+expectedPage);
                    });
                } finally {
                    removeMonitor(monitor);
                    if(reader!=null){final Activity opened=reader;runOnMainSync(opened::finish);waitForIdleSync();}
                }
            }
        } finally {runOnMainSync(main::finish);}
    }
    private void assertReaderState(Activity activity) {
        runOnMainSync(() -> {
            View root=activity.getWindow().getDecorView();
            if(!((android.widget.TextView)root.findViewWithTag("pdf-position")).getText().toString().startsWith("Página 44 de"))
                throw new AssertionError("Page lost on rotation");
            if(!root.findViewWithTag("pdf-page").getContentDescription().toString().contains("125%"))
                throw new AssertionError("Zoom lost on rotation");
        });
    }
    private void verifyRotation(Context context) throws Exception {
        Activity active=null;
        try {
            if(!getUiAutomation().setRotation(android.app.UiAutomation.ROTATION_FREEZE_0))
                throw new Exception("Cannot set initial portrait rotation");
            active=startActivitySync(new Intent(context,LocalPdfActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("path","data/pdfs/biblioteca/constituicoes-regra-diretorio-osm-2015.pdf").putExtra("page",43));
            waitForIdleSync();
            final Activity initial=active;
            runOnMainSync(() -> {
                View root=initial.getWindow().getDecorView();
                root.findViewWithTag("pdf-next").performClick();
                root.findViewWithTag("pdf-zoom-in").performClick();
            });
            assertReaderState(active);
            for(int rotation:new int[]{android.app.UiAutomation.ROTATION_FREEZE_90,android.app.UiAutomation.ROTATION_FREEZE_0}) {
                ActivityMonitor monitor=addMonitor(LocalPdfActivity.class.getName(),null,false);
                try {
                    if(!getUiAutomation().setRotation(rotation))throw new Exception("Cannot rotate PDF viewer");
                    Activity recreated=waitForMonitorWithTimeout(monitor,15000);
                    if(recreated==null||recreated==active)throw new Exception("PDF Activity was not recreated by rotation");
                    active=recreated;
                    waitForIdleSync();
                    int expected=rotation==android.app.UiAutomation.ROTATION_FREEZE_90?
                        android.content.res.Configuration.ORIENTATION_LANDSCAPE:android.content.res.Configuration.ORIENTATION_PORTRAIT;
                    if(active.getResources().getConfiguration().orientation!=expected)throw new Exception("Unexpected PDF orientation");
                    assertReaderState(active);
                } finally {removeMonitor(monitor);}
            }
        } finally {
            if(active!=null){final Activity last=active;runOnMainSync(last::finish);}
            getUiAutomation().setRotation(android.app.UiAutomation.ROTATION_UNFREEZE);
        }
    }
}
