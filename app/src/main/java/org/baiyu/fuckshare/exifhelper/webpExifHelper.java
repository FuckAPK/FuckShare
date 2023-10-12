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
    @Override
    public void removeMetadata(InputStream inputStream, OutputStream outputStream) {
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

            Set<String> chunkToRemove = Set.of("EXIF", "XMP ");

            byte[] webpHeader = new byte[12];
            byte[] chunkNameBytes = new byte[4];
            byte[] chunkDataLenBytes = new byte[4];
            long chunkDataLen;
            long realChunkDataLan;

            // calculate size
            // file size doesn't contain first 8 bytes
            long newSize = bis.available() - 8;
            bis.mark(bis.available());

            Utils.inputStreamSkip(bis, 12);
            while (bis.available() > 0) {
                Utils.inputStreamRead(bis, chunkNameBytes);
                String chunkName = new String(chunkNameBytes);
                Utils.inputStreamRead(bis, chunkDataLenBytes);

                chunkDataLen = Utils.littleEndianBytesToLong(chunkDataLenBytes);
                realChunkDataLan = chunkDataLen + (chunkDataLen % 2);

                Utils.inputStreamSkip(bis, realChunkDataLan);

                if (chunkToRemove.contains(chunkName)) {
                    newSize -= realChunkDataLan + 8;
                }
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

                chunkDataLen = Utils.littleEndianBytesToLong(chunkDataLenBytes);
                // standard of tiff: fill in end with 0x00 if chunk size if odd
                realChunkDataLan = chunkDataLen + (chunkDataLen % 2);

                if (chunkToRemove.contains(chunkName)) {
                    Utils.inputStreamSkip(bis, realChunkDataLan);
                    Log.d("fuckshare", "Discord chunk: " + chunkName + " size: " + realChunkDataLan);
                } else {
                    bos.write(chunkNameBytes);
                    bos.write(chunkDataLenBytes);
                    Utils.copy(bis, bos, realChunkDataLan);
                }
            }
            bos.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
