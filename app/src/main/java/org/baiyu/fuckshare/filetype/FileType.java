package org.baiyu.fuckshare.filetype;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

public interface FileType {
    String getExtension();
    Set<Map<Integer, byte[]>> getSignatures();

    default boolean isSupportMetadata() {
        return false;
    }

    default boolean signatureMatch(byte[] bytes) {
        Set<Map<Integer, byte[]>> signature = getSignatures();
        if (bytes == null || signature == null) {
            return false;
        }

        int signatureRequireLength = signature.stream().flatMap(m -> m.entrySet().stream()).map(
                integerEntry -> integerEntry.getKey() + integerEntry.getValue().length
        ).max(Comparator.comparingInt(x -> x)).orElse(0);
        if (signatureRequireLength <= 0 || bytes.length < signatureRequireLength) {
            return false;
        }

        return signature.stream().map(
                integerMap -> integerMap.entrySet().stream().map(
                        integerEntry -> {
                            int fixed = integerEntry.getKey();
                            byte[] signBytes = integerEntry.getValue();
                            for (int i = 0; i < signBytes.length; i++) {
                                if (bytes[i + fixed] != signBytes[i]) {
                                    return false;
                                }
                            }
                            return true;
                        }
                ).filter(b -> !b).findFirst().orElse(true)
        ).filter(s -> s).findFirst().orElse(false);
    }
}
