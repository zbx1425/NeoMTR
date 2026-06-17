package mtr.data;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Maps;
import net.minecraft.util.Util;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class MultipartName {

    private final String[] namesForUsages;

    private MultipartName(String[] namesForUsages) {
        this.namesForUsages = namesForUsages;
    }

    public String get(Usage usage) {
        if (namesForUsages[usage.ordinal()] != null) {
            return namesForUsages[usage.ordinal()];
        } else if (namesForUsages[0] != null) {
            return namesForUsages[0];
        } else {
            return "";
        }
    }

    public boolean isEmpty() {
        for (int i = 0; i < Usage.VALUES.length; i++) {
            if (namesForUsages[i] != null) {
                return false;
            }
        }
        return true;
    }

    public static MultipartName EMPTY = new MultipartName(new String[Usage.VALUES.length]);

    private static final Pattern PTN_COMMA = Pattern.compile(",");
    private static final Pattern PTN_COLON = Pattern.compile(":");
    public static MultipartName parse(String src) {
        if (src.isEmpty()) {
            return EMPTY;
        }
        String[] parts = PTN_COMMA.split(src);
        String[] namesForUsages = new String[Usage.VALUES.length];
        for (String part : parts) {
            if (part.contains(":")) {
                String[] usageAndPart = PTN_COLON.split(part, 2);
                Usage usage = Usage.BY_KEY.get(usageAndPart[0].trim().toLowerCase(Locale.ROOT));
                if (usage != null) {
                    String partTuple = usageAndPart[1].trim();
                    namesForUsages[usage.ordinal()] = partTuple;
                }
            } else {
                String partTuple = part.trim();
                namesForUsages[Usage.GENERIC.ordinal()] = partTuple;
            }
        }
        return new MultipartName(namesForUsages);
    }

    public MultipartName(FriendlyByteBuf buffer) {
        String[] namesForUsages = new String[Usage.VALUES.length];
        for (int i = 0; i < Usage.VALUES.length; i++) {
            if (buffer.readBoolean()) {
//                namesForUsages[usage.ordinal()] = new Names(
//                        buffer.readUtf(Names.PACKET_STRING_READ_LENGTH),
//                        buffer.readUtf(Names.PACKET_STRING_READ_LENGTH),
//                        buffer.readUtf(Names.PACKET_STRING_READ_LENGTH)
//                );
                namesForUsages[i] = buffer.readUtf(Names.PACKET_STRING_READ_LENGTH);
            }
        }
        this.namesForUsages = namesForUsages;
    }

    public void writePacket(FriendlyByteBuf buffer) {
        for (int i = 0; i < Usage.VALUES.length; i++) {
            String partTuple = namesForUsages[i];
            buffer.writeBoolean(partTuple != null);
            if (partTuple != null) {
//                buffer.writeUtf(partTuple.primaryName, Names.PACKET_STRING_READ_LENGTH);
//                buffer.writeUtf(partTuple.secondaryName, Names.PACKET_STRING_READ_LENGTH);
//                buffer.writeUtf(partTuple.hiddenName, Names.PACKET_STRING_READ_LENGTH);
                buffer.writeUtf(partTuple, Names.PACKET_STRING_READ_LENGTH);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Usage usage : Usage.VALUES) {
            String partTuple = namesForUsages[usage.ordinal()];
            if (partTuple != null) {
                if (!sb.isEmpty()) sb.append(",");
                if (usage != Usage.GENERIC) {
                    sb.append(usage.serializeKey).append(":");
                }
                sb.append(partTuple);
//                if (!partTuple.secondaryName.isEmpty()) {
//                    sb.append("|").append(partTuple.secondaryName);
//                }
//                if (!partTuple.hiddenName.isEmpty()) {
//                    sb.append("||").append(partTuple.hiddenName);
//                }
            }
        }
        return sb.toString();
    }

    public static class Names {
        public final String primaryName;
        public final String secondaryName;
        public final String hiddenName;

        public static final int PACKET_STRING_READ_LENGTH = 32767;
        public static final Names EMPTY = new Names("", "", "");

        public Names(String primaryName, String secondaryName, String hiddenName) {
            this.primaryName = primaryName;
            this.secondaryName = secondaryName;
            this.hiddenName = hiddenName;
        }

        private static final Pattern PTN_DOUBLE_BAR = Pattern.compile("\\|\\|");
        private static final Pattern PTN_BAR = Pattern.compile("\\|");
        public static Names parse(String src) {
            String[] visibleAndHidden = PTN_DOUBLE_BAR.split(src, 2);
            String[] primaryAndSecondary = PTN_BAR.split(visibleAndHidden[0]);
            return new Names(
                    primaryAndSecondary[0],
                    primaryAndSecondary.length > 1 ? primaryAndSecondary[1] : "",
                    visibleAndHidden.length > 1 ? visibleAndHidden[1] : ""
            );
        }
    }

    public enum Usage {
        GENERIC,
        MAP_DEST,
        PIDS_DEST,
        TRAIN_DEST;

        public final String serializeKey;
        Usage() {
            serializeKey = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
        }

        public static final Usage[] VALUES = values();
        public static final Map<String, Usage> BY_KEY = Util.make(Maps.newHashMap(), map -> {
            for (Usage usage : VALUES) {
                map.put(usage.serializeKey.toLowerCase(Locale.ROOT), usage);
            }
        });
    }
}
