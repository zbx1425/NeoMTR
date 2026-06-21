package cn.zbx1425.mtrsteamloco;

import cn.zbx1425.mtrsteamloco.data.EyeCandyProperties;
import cn.zbx1425.mtrsteamloco.data.EyeCandyRegistry;
import cn.zbx1425.sowcerext.model.ModelCluster;
import cn.zbx1425.sowcerext.model.RawModel;
import cn.zbx1425.sowcerext.model.loader.NmbModelLoader;
import mtr.mappings.Text;
import net.minecraft.resources.Identifier;
import org.apache.commons.io.FilenameUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class Debug {

    public static void saveAllLoadedModels(Path outputDir) {
        for (Map.Entry<Identifier, RawModel> pair : MainClient.modelManager.loadedRawModels.entrySet()) {
            Path path = Paths.get(outputDir.toString(), pair.getKey().getNamespace(), pair.getKey().getPath());
            try {
                Files.createDirectories(path.getParent());
                FileOutputStream fos = new FileOutputStream(FilenameUtils.removeExtension(path.toString()) + ".nmb");
                NmbModelLoader.serializeModel(pair.getValue(), fos, false);
                fos.close();
            } catch (IOException e) {
                Main.LOGGER.error("Failed exporting models:", e);
            }
        }
    }

    public static void registerAllModelsAsEyeCandy() {
        for (Map.Entry<Identifier, ModelCluster> entry : MainClient.modelManager.uploadedVertArrays.entrySet()) {
            String key = FilenameUtils.getBaseName(entry.getKey().getPath());
            EyeCandyRegistry.register(key, new EyeCandyProperties(Text.literal(key), entry.getValue(), null));
        }
    }
}
