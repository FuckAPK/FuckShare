package org.baiyu.fuckshare;

import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ExifHelper {
    private static final Set<String> privacyTags = Set.of(
            "DateTime",
            "ImageDescription",
            "Make",
            "Model",
            "Software",
            "Artist",
            "Copyright",
            "ExifVersion",
            "FlashpixVersion",
            "ColorSpace",
            "MakerNote",
            "UserComment",
            "RelatedSoundFile",
            "DateTimeOriginal",
            "DateTimeDigitized",
            "OffsetTime",
            "OffsetTimeOriginal",
            "OffsetTimeDigitized",
            "SubSecTime",
            "SubSecTimeOriginal",
            "SubSecTimeDigitized",
            "ExposureTime",
            "FNumber",
            "ExposureProgram",
            "SpectralSensitivity",
            "ISOSpeedRatings",
            "PhotographicSensitivity",
            "OECF",
            "SensitivityType",
            "StandardOutputSensitivity",
            "RecommendedExposureIndex",
            "ISOSpeed",
            "ISOSpeedLatitudeyyy",
            "ISOSpeedLatitudezzz",
            "ShutterSpeedValue",
            "ApertureValue",
            "BrightnessValue",
            "ExposureBiasValue",
            "MaxApertureValue",
            "SubjectDistance",
            "MeteringMode",
            "LightSource",
            "Flash",
            "SubjectArea",
            "FocalLength",
            "FlashEnergy",
            "SpatialFrequencyResponse",
            "SubjectLocation",
            "ExposureIndex",
            "SensingMethod",
            "FileSource",
            "SceneType",
            "CFAPattern",
            "CustomRendered",
            "ExposureMode",
            "WhiteBalance",
            "DigitalZoomRatio",
            "FocalLengthIn35mmFilm",
            "SceneCaptureType",
            "GainControl",
            "Contrast",
            "Saturation",
            "Sharpness",
            "DeviceSettingDescription",
            "SubjectDistanceRange",
            "ImageUniqueID",
            "CameraOwnerName",
            "BodySerialNumber",
            "LensSpecification",
            "LensMake",
            "LensModel",
            "LensSerialNumber",
            "GPSVersionID",
            "GPSLatitudeRef",
            "GPSLatitude",
            "GPSLongitudeRef",
            "GPSLongitude",
            "GPSAltitudeRef",
            "GPSAltitude",
            "GPSTimeStamp",
            "GPSSatellites",
            "GPSStatus",
            "GPSMeasureMode",
            "GPSDOP",
            "GPSSpeedRef",
            "GPSSpeed",
            "GPSTrackRef",
            "GPSTrack",
            "GPSImgDirectionRef",
            "GPSImgDirection",
            "GPSMapDatum",
            "GPSDestLatitudeRef",
            "GPSDestLatitude",
            "GPSDestLongitudeRef",
            "GPSDestLongitude",
            "GPSDestBearingRef",
            "GPSDestBearing",
            "GPSDestDistanceRef",
            "GPSDestDistance",
            "GPSProcessingMethod",
            "GPSAreaInformation",
            "GPSDateStamp",
            "GPSDifferential",
            "GPSHPositioningError",
            "InteroperabilityIndex",
            "DNGVersion",
            "DefaultCropSize",
            "ThumbnailImage",
            "PreviewImageStart",
            "PreviewImageLength",
            "AspectFrame",
            "SensorBottomBorder",
            "SensorLeftBorder",
            "SensorRightBorder",
            "SensorTopBorder",
            "ISO",
            "JpgFromRaw",
            "Xmp",
            "NewSubfileType",
            "SubfileType"
    );

    private static final Set<String> pngCriticalChunks = Set.of(
            "acTL", //   animation control
            "bKGD", //   background color
            "cHRM", //   Primary Chromaticities
            "gAMA", //   Gamma
            "gIFg", //   GIFGraphicControlExtension
            "gIFt", //   GIFPlainTextExtension
            "gIFx", //   GIFApplicationExtension
            "fcTL", // * frame control
            "fdAT", // * frame data
            "hIST", //   PaletteHistogram
            "IDAT", // * image data
            "IEND", // * trailer
            "IHDR", // * header
            "pCAL", //   Pixel Calibration
            "pHYs", //   PhysicalPixel
            "PLTE", // * palette
            "sBIT", //   SignificantBits
            "sCAL", //   SubjectScale
            "sRGB", //   SRGBRendering
            "sTER", //   StereoImage
            "tRNS", //   Transparency
            "vpAg"  //   VirtualPage
    );

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

    private ExifHelper() {
    }

    private static void removeMetadata(ExifInterface exifInterface, Set<String> tagsToRemove) throws IOException {
        Set<String> tagsHasToRemove = tagsToRemove.stream()
                .filter(exifInterface::hasAttribute).collect(Collectors.toSet());
        tagsHasToRemove.forEach(tag -> exifInterface.setAttribute(tag, null));
        Log.d("fuckshare", "tags to remove: " + tagsHasToRemove);
        exifInterface.saveAttributes();
    }

    public static void removeMetadataExclude(ExifInterface exifInterface, Set<String> excludeTags) throws IOException {
        Set<String> newTagSet = new HashSet<>(privacyTags);
        Log.d("fuckshare", "all tags " + newTagSet);
        excludeTags.forEach(newTagSet::remove);
        removeMetadata(exifInterface, newTagSet);
    }

    public static void pngToNewWithoutMetadata(InputStream inputStream, OutputStream outputStream) {
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

            byte[] byteArray = new byte[8];
            Utils.inputStreamRead(bis, byteArray);
            bos.write(byteArray);

            byte[] chunkLengthBytes = new byte[4];
            byte[] chunkNameBytes = new byte[4];
            long chunkLength;

            while (bis.available() > 0) {
                Utils.inputStreamRead(bis, chunkLengthBytes);
                chunkLength = Utils.bigEndianBytesToLong(chunkLengthBytes);

                Utils.inputStreamRead(bis, chunkNameBytes);
                String chunkName = new String(chunkNameBytes);

                if (pngCriticalChunks.contains(chunkName)) {
                    bos.write(chunkLengthBytes);
                    bos.write(chunkNameBytes);
                    Utils.copy(bis, bos, chunkLength + 4);
                } else {
                    // skip chunkData and chunkCrc
                    Utils.inputStreamSkip(bis, chunkLength + 4);
                    Log.d("fuckshare", "Discord chunk: " + chunkName + " size: " + chunkLength + 8);
                }
                if (chunkName.equals("IEND")) {
                    break;
                }
            }
            bos.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void jpegToNewWithoutMetadata(InputStream inputStream, OutputStream outputStream) {
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
                    len = Utils.copy(bis, bos);
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

    public static void webpToNewWithoutMetadata(InputStream inputStream, OutputStream outputStream) {
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
            bis.mark(bis.available() + 1);

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

    public static void writeBackMetadata(File from, File to, Set<String> tags) throws IOException {
        ExifInterface exifFrom = new ExifInterface(from);
        ExifInterface exifTo = new ExifInterface(to);
        writeBackMetadata(exifFrom, exifTo, tags);
    }

    public static void writeBackMetadata(ExifInterface exifFrom, ExifInterface exifTo, Set<String> tags) throws IOException {
        tags.stream()
                .filter(exifFrom::hasAttribute)
                .forEach(tag -> {
                    exifTo.setAttribute(tag, exifFrom.getAttribute(tag));
                    Log.d("fuckshare", "writing back tag: " + tag);
                });
        // TODO seems not working
        exifTo.saveAttributes();
    }
}
