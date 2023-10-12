package org.baiyu.fuckshare.exifhelper;

import android.os.FileUtils;
import android.util.Log;

import org.baiyu.fuckshare.Utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public class jpegExifHelper implements ExifHelper {

    private static final Set<Byte> jpegSkippableChunks = Set.of(
            (byte) 0xE0,   //
            (byte) 0xE1,   // exif, xmp, xap
            (byte) 0xE2,   // icc
            (byte) 0xE3,   // Kodak
            (byte) 0xE4,   // FlashPix
            (byte) 0xE5,   // Ricoh
            (byte) 0xE6,   // GoPro
            (byte) 0xE7,   // Pentax/Qualcomm
            (byte) 0xE8,   // Spiff
            (byte) 0xE9,   // MediaJukebox
            (byte) 0xEA,   // PhotoStudio
            (byte) 0xEB,   // HDR
            (byte) 0xEC,   // photoshoP ducky / savE foR web
            (byte) 0xED,   // photoshoP savE As
            (byte) 0xEE,   // "adobe" (length = 12)
            (byte) 0xEF,   // GraphicConverter
            (byte) 0xFE    // Comments
    );

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

            byte[] maker = new byte[2];
            byte[] lenBytes = new byte[2];
            long len;

            while (bis.available() > 0) {
                Utils.inputStreamRead(bis, maker);
                assert maker[0] == (byte) 0xFF;

                if (maker[1] == (byte) 0xD8) {
                    bos.write(maker);
                    Log.d("fuckshare", String.format("Chunk copied: %02X size: " + 2, maker[1]));
                } else if (maker[1] == (byte) 0xD9) {   // EOI
                    bos.write(maker);
                    Log.d("fuckshare", String.format("Chunk copied: %02X size: " + 2, maker[1]));
                    break;
                } else if (jpegSkippableChunks.contains(maker[1])) {
                    Utils.inputStreamRead(bis, lenBytes);
                    len = Utils.bigEndianBytesToLong(lenBytes) - 2;
                    Utils.inputStreamSkip(bis, len);
                    Log.d("fuckshare", String.format("Discord chunk: %02X size: " + (len + 4), maker[1]));
                } else if (maker[1] == (byte) 0xDA) {   // SOS
                    bos.write(maker);
                    // write all data
                    len = FileUtils.copy(bis, bos);
                    Log.d("fuckshare", "DA and following Chunks copied size: " + (len + 2));
                } else {
                    Utils.inputStreamRead(bis, lenBytes);
                    len = Utils.bigEndianBytesToLong(lenBytes) - 2;
                    bos.write(maker);
                    bos.write(lenBytes);
                    Utils.copy(bis, bos, len);
                    Log.d("fuckshare", String.format("Chunk copied: %02X size: " + (len + 4), maker[1]));
                }
            }
            bos.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
