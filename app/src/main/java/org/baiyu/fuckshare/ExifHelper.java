package org.baiyu.fuckshare;

import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExifHelper {
    private static final List<String> privacyTags = List.of(
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

    private ExifHelper() {
    }

    private static void removeMetadata(ExifInterface exifInterface, List<String> tagsToRemove) throws IOException {
        List<String> tagsHasToRemove = tagsToRemove.stream()
                .filter(exifInterface::hasAttribute).collect(Collectors.toList());
        tagsHasToRemove.forEach(tag -> exifInterface.setAttribute(tag, null));
        Log.d("fuckshare", "tags to remove: " + tagsHasToRemove);
        exifInterface.saveAttributes();
    }

    public static void removeMetadataExclude(ExifInterface exifInterface, List<String> excludeTags) throws IOException {
        List<String> newTagList = new ArrayList<>(privacyTags);
        Log.d("fuckshare", "all tags " + newTagList);
        newTagList.removeAll(excludeTags);
        removeMetadata(exifInterface, newTagList);
    }

}
