package org.baiyu.fuckshare;

import android.content.SharedPreferences;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.robv.android.xposed.XSharedPreferences;

public class Settings {

    private static final String PREF_ENABLE_FORCE_FORWARD_HOOK = "enable_force_forward_hook";
    private static final String PREF_ENABLE_REMOVE_EXIF = "enable_remove_exif";
    private static final String PREF_EXIF_TAGS_TO_KEEP = "exif_tags_to_keep";
    private static final String PREF_ENABLE_IMAGE_RENAME = "enable_image_rename";
    private static final String PREF_ENABLE_FILE_RENAME = "enable_file_rename";
    private volatile static Settings INSTANCE;
    private final SharedPreferences prefs;

    private Settings(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public static Settings getInstance(SharedPreferences prefs) {
        if (INSTANCE == null) {
            synchronized (Settings.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Settings(prefs);
                }
            }
        }
        return INSTANCE;
    }

    public boolean enableForceForwardHook() {
        if (prefs instanceof XSharedPreferences xprefs) {
            xprefs.reload();
        }
        return prefs.getBoolean(PREF_ENABLE_FORCE_FORWARD_HOOK, false);
    }

    public boolean enableRemoveExif() {
        return prefs.getBoolean(PREF_ENABLE_REMOVE_EXIF, true);
    }

    public Set<String> getExifTagsToKeep() {
        String rawPref = prefs.getString(PREF_EXIF_TAGS_TO_KEEP, "Orientation, Gamma, ColorSpace, XResolution, YResolution, ResolutionUnit");
        return Stream.of(rawPref.split("[, ]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    public boolean enableImageRename() {
        return prefs.getBoolean(PREF_ENABLE_IMAGE_RENAME, true);
    }

    public boolean enableFileRename() {
        return prefs.getBoolean(PREF_ENABLE_FILE_RENAME, false);
    }
}
