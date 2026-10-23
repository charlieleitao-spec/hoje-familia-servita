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
            for (String directory : new String[]{"data/pdfs/missal", "data/pdfs/oficios", "recursos/pdfs"}) {
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
            if (files != 76 || pages < 342) throw new Exception("PDF inventory " + files + "/" + pages);
            Activity viewer = startActivitySync(new Intent(context, LocalPdfActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("path", "recursos/pdfs/folha-servita-ano-completo.pdf")
                .putExtra("page", 2).putExtra("title", "Folhas do ano"));
            waitForIdleSync();
            runOnMainSync(() -> {
                View root = viewer.getWindow().getDecorView();
                if (!((android.widget.TextView) root.findViewWithTag("pdf-position")).getText().toString().startsWith("Página 2 de")) throw new AssertionError("Initial page");
                root.findViewWithTag("pdf-zoom-in").performClick();
                if (!root.findViewWithTag("pdf-page").getContentDescription().toString().contains("125%")) throw new AssertionError("Zoom");
                root.findViewWithTag("pdf-print").performClick();
            });
            boolean opened = false;
            for (int attempt = 0; attempt < 60; attempt++) {
                android.view.accessibility.AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
                if (root != null && "com.android.printspooler".equals(String.valueOf(root.getPackageName()))) { opened = true; break; }
                Thread.sleep(250);
            }
            if (!opened) throw new Exception("Print UI did not open");
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
            result.putString("validation", "PASS");
            result.putString("stream", "PASS: " + files + " PDFs; " + pages + " pages; original print bytes; zoom; Android printing; cancel and return.\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("validation", "FAIL");
            result.putString("stream", android.util.Log.getStackTraceString(error));
            finish(Activity.RESULT_CANCELED, result);
        }
    }
}
