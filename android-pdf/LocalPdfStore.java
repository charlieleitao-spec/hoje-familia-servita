package br.org.servitas.familia;

import android.content.Context;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

/** Opens only packaged PDFs. No HTTP, storage permission or external viewer. */
public final class LocalPdfStore {
    public static boolean isValidPath(String path) {
        return path != null && path.matches("(?:data/pdfs/(?:missal|oficios)/[a-z0-9-]+|recursos/pdfs/folha-servita-[a-z0-9-]+)\\.pdf");
    }
    public static PdfRenderer open(Context context, String path) throws IOException {
        if(!isValidPath(path))
            throw new IOException("PDF local inválido");
        File directory=new File(context.getCacheDir(),"osm-pdfs");
        if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cache indisponível");
        File temporary=File.createTempFile("pdf-", ".tmp", directory);
        File file=new File(directory,path.replace('/', '_'));
        try {
            try(InputStream input=context.getAssets().open("public/"+path);
                FileOutputStream output=new FileOutputStream(temporary)){
                byte[] buffer=new byte[16384];int read;
                while((read=input.read(buffer))!=-1)output.write(buffer,0,read);
            }
            if(file.exists()&&!file.delete())throw new IOException("Falha ao atualizar PDF local");
            if(!temporary.renameTo(file))throw new IOException("Falha ao preparar PDF local");
            ParcelFileDescriptor descriptor=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
            try {return new PdfRenderer(descriptor);}
            catch(IOException|RuntimeException error){descriptor.close();throw error;}
        } finally {temporary.delete();}
    }
}
