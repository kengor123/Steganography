package comp.proj.painter.ui.code;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteSource extends org.apache.commons.imaging.common.bytesource.ByteSource {

    Context context;
    Uri uri;

    public ByteSource(Context context, Uri uri){
        super(uri.toString());
        this.context = context;
        this.uri = uri;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return context.getContentResolver().openInputStream(uri);
    }

    @Override
    public byte[] getBlock(long start, int length) throws IOException {
        return getBlock(0xFFFFffffL & start, length);
    }

    @Override
    public byte[] getAll() throws IOException {
        ByteBuffer b = ByteBuffer.allocate(Integer.MAX_VALUE);
        int bb = 0;
        while ((bb = getInputStream().read()) != -1) {
            b.put((byte)bb);
        }
        return b.array();
    }


    @Override
    public long getLength() throws IOException {
        return getAll().length;
    }

    @Override
    public String getDescription() {
        return "";
    }
}


