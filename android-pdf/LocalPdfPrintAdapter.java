package br.org.servitas.familia;

import android.content.Context;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/** Spools the original PDF bytes; Android handles page selection and printer settings. */
public final class LocalPdfPrintAdapter extends PrintDocumentAdapter {
    private final Context context;
    private final String path;
    private final int pageCount;
    public LocalPdfPrintAdapter(Context context,String path,int pageCount){
        this.context=context.getApplicationContext();this.path=path;this.pageCount=pageCount;
    }
    @Override public void onLayout(PrintAttributes oldAttributes,PrintAttributes newAttributes,CancellationSignal cancellation,LayoutResultCallback callback,Bundle extras){
        if(cancellation.isCanceled()){callback.onLayoutCancelled();return;}
        callback.onLayoutFinished(new PrintDocumentInfo.Builder(path.substring(path.lastIndexOf('/')+1))
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(pageCount).build(),false);
    }
    public static void writeOriginal(Context context,String path,OutputStream output,CancellationSignal cancellation) throws IOException {
        if(!LocalPdfStore.isValidPath(path))throw new IOException("PDF local inválido");
        cancellation.throwIfCanceled();
        try(InputStream input=context.getAssets().open("public/"+path)){
            byte[] buffer=new byte[16384];int read;
            while((read=input.read(buffer))!=-1){cancellation.throwIfCanceled();output.write(buffer,0,read);}
        }
        cancellation.throwIfCanceled();
    }
    @Override public void onWrite(PageRange[] pages,ParcelFileDescriptor destination,CancellationSignal cancellation,WriteResultCallback callback){
        Handler main=new Handler(Looper.getMainLooper());
        new Thread(()->{
            try(OutputStream output=new ParcelFileDescriptor.AutoCloseOutputStream(destination)){
                writeOriginal(context,path,output,cancellation);
            }catch(android.os.OperationCanceledException error){main.post(callback::onWriteCancelled);return;}
            catch(Exception error){main.post(()->callback.onWriteFailed("Não foi possível preparar o PDF para impressão."));return;}
            main.post(()->{if(cancellation.isCanceled())callback.onWriteCancelled();else callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});});
        },"osm-pdf-print").start();
    }
}
