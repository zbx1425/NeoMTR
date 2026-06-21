package cn.zbx1425.sowcerext.reuse;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.sowcerext.model.RawMesh;
import cn.zbx1425.sowcerext.util.ResourceUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class AtlasManager {

    public HashMap<Identifier, AtlasSprite> sprites = new HashMap<>();

    public HashSet<Identifier> noAtlasList = new HashSet<>();

    public void load(ResourceManager resourceManager, Identifier atlasConf) throws IOException {
        JsonObject atlasConfObj = Main.JSON_PARSER.parse(ResourceUtil.readResource(resourceManager, atlasConf)).getAsJsonObject();
        String basePath = atlasConfObj.get("basePath").getAsString();
        for (JsonElement sheetObj : atlasConfObj.get("sheets").getAsJsonArray()) {
            Identifier sheetConf = ResourceUtil.resolveRelativePath(atlasConf, sheetObj.getAsString(), ".json");
            Identifier sheetTexture = ResourceUtil.resolveRelativePath(atlasConf, sheetObj.getAsString(), ".png");
            JsonObject sheetConfObj = Main.JSON_PARSER.parse(ResourceUtil.readResource(resourceManager, sheetConf)).getAsJsonObject();
            int sheetWidth = sheetConfObj.get("meta").getAsJsonObject().get("size").getAsJsonObject().get("w").getAsInt();
            int sheetHeight = sheetConfObj.get("meta").getAsJsonObject().get("size").getAsJsonObject().get("h").getAsInt();
            for (Map.Entry<String, JsonElement> entry : sheetConfObj.get("frames").getAsJsonObject().entrySet()) {
                Identifier texture = ResourceUtil.resolveRelativePath(sheetConf, basePath + entry.getKey(), ".png");
                JsonObject spriteObj = entry.getValue().getAsJsonObject();
                sprites.put(texture, new AtlasSprite(
                        sheetTexture, sheetWidth, sheetHeight,
                        spriteObj.get("frame").getAsJsonObject().get("x").getAsInt(), spriteObj.get("frame").getAsJsonObject().get("y").getAsInt(),
                        spriteObj.get("frame").getAsJsonObject().get("w").getAsInt(), spriteObj.get("frame").getAsJsonObject().get("h").getAsInt(),
                        spriteObj.get("spriteSourceSize").getAsJsonObject().get("x").getAsInt(), spriteObj.get("spriteSourceSize").getAsJsonObject().get("y").getAsInt(),
                        spriteObj.get("spriteSourceSize").getAsJsonObject().get("w").getAsInt(), spriteObj.get("spriteSourceSize").getAsJsonObject().get("h").getAsInt(),
                        spriteObj.get("sourceSize").getAsJsonObject().get("w").getAsInt(), spriteObj.get("sourceSize").getAsJsonObject().get("h").getAsInt(),
                        spriteObj.get("rotated").getAsBoolean()
                ));
            }
        }
        for (JsonElement noAtlasObj : atlasConfObj.get("noAtlas").getAsJsonArray()) {
            Identifier noAtlasTexture = ResourceUtil.resolveRelativePath(atlasConf, basePath + noAtlasObj.getAsString(), ".png");
            noAtlasList.add(noAtlasTexture);
        }
    }

    public void applyToMesh(RawMesh mesh) {
        if (mesh.materialProp.texture == null || noAtlasList.contains(mesh.materialProp.texture)) return;
        AtlasSprite sprite = sprites.getOrDefault(mesh.materialProp.texture, null);
        if (sprite != null) sprite.applyToMesh(mesh);
    }

    public void clear() {
        sprites.clear();
    }

}
