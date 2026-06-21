package cn.zbx1425.sowcerext.util;

import mtr.util.UtilitiesClient;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.util.List;
import java.util.Locale;

public class ResourceUtil {

    public static String readResource(ResourceManager manager, Identifier location) throws IOException {
        final List<Resource> resources = UtilitiesClient.getResources(manager, location);
        if (resources.isEmpty()) return "";
        return IOUtils.toString(new BOMInputStream(resources.get(0).open()), StandardCharsets.UTF_8);
    }

    public static Identifier resolveRelativePath(Identifier baseFile, String relative, String expectExtension) {
        relative = relative.toLowerCase(Locale.ROOT).replace('\\', '/');

        if (relative.contains(":")) {
            relative = relative.replaceAll("[^a-z0-9/.:_-]", "_");
            return Identifier.parse(relative);
        }

        relative = relative.replaceAll("[^a-z0-9/._-]", "_");

        if (relative.endsWith(".jpg") || relative.endsWith(".bmp") || relative.endsWith(".tga")) {
            relative = relative.substring(0, relative.length() - 4) + ".png";
        }

        if (expectExtension != null && !relative.endsWith(expectExtension)) {
            relative += expectExtension;
        }
        String resolvedPath = FileSystems.getDefault().getPath(baseFile.getPath()).getParent().resolve(relative)
                .normalize().toString().replace('\\', '/');
        return Identifier.fromNamespaceAndPath(baseFile.getNamespace(), resolvedPath);
    }
}
