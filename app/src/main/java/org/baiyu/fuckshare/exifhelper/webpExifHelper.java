package org.baiyu.fuckshare.exifhelper;

import android.util.Log;

import org.baiyu.fuckshare.Utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public class webpExifHelper implements ExifHelper {
    private static final Set<String> webpSkippableChunks = Set.of(
            "EXIF",
            "XMP "
    );

    @Override
    public void removeMetadata(InputStream inputStream, OutputStream outputStream) throws IOException, ImageFormatException {
        try {
            BufferedInputStream bis;
            BufferedOutputStream bos;

            if (inputStream instanceof BufferedInputStream) {
                bis = (BufferedInputStream) inputStream;
            } else {
                bis = new BufferedInputStream(inputStream);
            }

            if (outputStream instanceof BufferedOutputStream) {
                bos = (BufferedOutputStream) outputStream;
            } else {
                bos = new BufferedOutputStream(outputStream);
            }

            byte[] webpHeader = new byte[12];
            byte[] chunkNameBytes = new byte[4];
            byte[] chunkDataLenBytes = new byte[4];
            long realChunkDataLength;

            // calculate size
            // file size doesn't contain first 8 bytes
            long newSize = bis.available() - 8;
            bis.mark(bis.available());

            Utils.inputStreamSkip(bis, 12);
            while (bis.available() > 0) {
                Utils.inputStreamRead(bis, chunkNameBytes);
                String chunkName = new String(chunkNameBytes);
                Utils.inputStreamRead(bis, chunkDataLenBytes);

                realChunkDataLength = Utils.littleEndianBytesToLong(chunkDataLenBytes);
                realChunkDataLength += realChunkDataLength % 2;

                if (webpSkippableChunks.contains(chunkName)) {
                    newSize -= realChunkDataLength + 8;
                }
                realChunkDataLength -= Utils.inputStreamSkip(bis, realChunkDataLength);
                assert realChunkDataLength == 0;
            }

            bis.reset();

            // rewrite with new size
            Utils.inputStreamRead(bis, webpHeader);
            bos.write(webpHeader, 0, 4);
            bos.write(Utils.longToLittleEndianBytes(newSize), 0, 4);
            bos.write(webpHeader, 8, 4);

            while (bis.available() > 0) {
                Utils.inputStreamRead(bis, chunkNameBytes);
                String chunkName = new String(chunkNameBytes);
                Utils.inputStreamRead(bis, chunkDataLenBytes);

                realChunkDataLength = Utils.littleEndianBytesToLong(chunkDataLenBytes);
                // standard of tiff: fill in end with 0x00 if chunk size if odd
                realChunkDataLength += realChunkDataLength % 2;

                if (webpSkippableChunks.contains(chunkName)) {
                    realChunkDataLength -= Utils.inputStreamSkip(bis, realChunkDataLength);
                    Log.d("fuckshare", "Discord chunk: " + chunkName + " size: " + realChunkDataLength);
                } else {
                    bos.write(chunkNameBytes);
                    bos.write(chunkDataLenBytes);
                    realChunkDataLength -= Utils.copy(bis, bos, realChunkDataLength);
                }
                assert realChunkDataLength == 0;
            }
            bos.flush();
        } catch (AssertionError error) {
            throw new ImageFormatException();
        }
    }
}
