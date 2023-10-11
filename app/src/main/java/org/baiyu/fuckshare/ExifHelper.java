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
            bis.read(byteArray);
            bos.write(byteArray);

            byte[] chunkLengthBytes = new byte[4];
            byte[] chunkNameBytes = new byte[4];

            while (bis.available() > 0) {
                bis.read(chunkLengthBytes);
                int chunkLength = bytesToUInt(chunkLengthBytes);

                bis.read(chunkNameBytes);
                String chunkName = new String(chunkNameBytes);

                if (pngCriticalChunks.contains(chunkName)) {
                    bos.write(chunkLengthBytes);
                    bos.write(chunkNameBytes);
                    Utils.copy(bis, bos, chunkLength + 4);
                } else {
                    // skip chunkData and chunkCrc
                    inputStreamSkip(bis, chunkLength + 4);
                    Log.d("fuckshare", "Discord chunk " + chunkName);
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
            int len;

            loop:
            while (bis.available() > 0) {
                if (maker[1] == (byte) 0xDA) {
                    while (true) {
                        bis.read(maker, 0, 1);
                        while (maker[0] != (byte) 0xFF) {
                            bos.write(maker, 0, 1);
                            bis.read(maker, 0, 1);
                        }
                        bis.read(maker, 1, 1);
                        if (maker[1] == (byte) 0x00) {
                            bos.write(maker);
                            continue;
                        }
                        break;
                    }
                } else {
                    bis.read(maker);
                }
                assert maker[0] == (byte) 0xFF;
                switch (maker[1]) {
                    case (byte) 0xD0:   // RST0..7
                    case (byte) 0xD1:
                    case (byte) 0xD2:
                    case (byte) 0xD3:
                    case (byte) 0xD4:
                    case (byte) 0xD5:
                    case (byte) 0xD6:
                    case (byte) 0xD7:
                    case (byte) 0xD8:   // SOI
                        bos.write(maker);
                        break;
                    case (byte) 0xD9:   // EOI
                        bos.write(maker);
                        break loop;
                    case (byte) 0xDD:   // DRI
                        bos.write(maker);
                        Utils.copy(bis, bos, 4);
                        break;
                    case (byte) 0xE1:   // exif, xmp, xap
                    case (byte) 0xE2:   // icc
                    case (byte) 0xE3:   // Kodak
                    case (byte) 0xE4:   // FlashPix
                    case (byte) 0xE5:   // Ricoh
                    case (byte) 0xE6:   // GoPro
                    case (byte) 0xE7:   // Pentax/Qualcomm
                    case (byte) 0xE8:   // Spiff
                    case (byte) 0xE9:   // MediaJukebox
                    case (byte) 0xEA:   // PhotoStudio
                    case (byte) 0xEB:   // HDR
                    case (byte) 0xEC:   // photoshoP ducky / savE foR web
                    case (byte) 0xED:   // photoshoP savE As
                    case (byte) 0xEE:   // "adobe" (length = 12)
                    case (byte) 0xEF:   // GraphicConverter
                        bis.read(lenBytes);
                        len = bytesToUInt(lenBytes) - 2;
                        inputStreamSkip(bis, len);
                        break;
                    case (byte) 0xC0:   // SOF0
                    case (byte) 0xC2:   // SOF2
                    case (byte) 0xC4:   // DHT
                    case (byte) 0xDB:   // DQT
                    case (byte) 0xDA:   // SOS
                        bis.read(lenBytes);
                        len = bytesToUInt(lenBytes) - 2;
                        bos.write(maker);
                        bos.write(lenBytes);
                        Utils.copy(bis, bos, len);
                        break;
                    default:
                        bis.read(lenBytes);
                        len = bytesToUInt(lenBytes) - 2;
                        bos.write(maker);
                        bos.write(lenBytes);
                        Utils.copy(bis, bos, len);
                        Log.d("fuckshare", String.format("unknown Copied: %02X , len: " + len, maker[1]));
                        break;
                }
            }
            bos.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void webpToNewWithoutMetadata(InputStream is, OutputStream os) {
        // TODO

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

    private static int bytesToUInt(byte[] bytes) {
        int value = 0;
        for (byte b : bytes) {
            value = (value << 8) | (0xFF & b);
        }
        return value;
    }

    private static long inputStreamSkip(InputStream inputStream, long n) throws IOException {
        long remaining = n;
        while (inputStream.available() > 0 && remaining > 0) {
            remaining -= inputStream.skip(remaining);
        }
        return n - remaining;
    }

}
