package se.mickelus.tetra.client.model.crap;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.IPerspectiveAwareModel;
import net.minecraftforge.client.model.ItemLayerModel;
import net.minecraftforge.client.model.ModelStateComposition;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;

import java.util.Collection;

import javax.vecmath.Vector3f;

public class ModuleModel implements IModel {

    protected final int offsetX;
    protected final int offsetY;

    private final ImmutableList<ResourceLocation> textures;

    public ModuleModel(ImmutableList<ResourceLocation> textures) {
        this(textures, 0, 0);
    }

    public ModuleModel(ImmutableList<ResourceLocation> textures, int offsetX, int offsetY) {
        this.textures = textures;

        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    @Override
    public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        return bakeIt(state, format, bakedTextureGetter);
    }

    // the only difference here is the return-type
    public BakedModuleModel bakeIt(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        // take offset of texture into account
        if(offsetX != 0 || offsetY != 0) {
            state = new ModelStateComposition(state, TRSRTransformation
                    .blockCenterToCorner(new TRSRTransformation(new Vector3f(offsetX / 16f, -offsetY / 16f, 0), null, null, null)));
        }
        ImmutableMap<TransformType, TRSRTransformation> map = IPerspectiveAwareModel.MapWrapper.getTransforms(state);

        // normal model as the base
        IBakedModel base = new ItemLayerModel(textures).bake(state, format, bakedTextureGetter);

        // turn it into a baked material-model
        BakedModuleModel bakedMaterialModel = new BakedModuleModel(base, map);

        // and generate the baked model for each material-variant we have for the base texture
        String baseTexture = base.getParticleTexture().getIconName();
//        Map<String, TextureAtlasSprite> sprites = CustomTextureCreator.sprites.get(baseTexture);
//
//        if(sprites != null) {
//            for(Map.Entry<String, TextureAtlasSprite> entry : sprites.entrySet()) {
//                Material material = TinkerRegistry.getMaterial(entry.getKey());
//
//                IModel model2 = ItemLayerModel.INSTANCE.retexture(ImmutableMap.of("layer0", entry.getValue().getIconName()));
//                IBakedModel bakedModel2 = model2.bake(state, format, bakedTextureGetter);
//
//                // if it's a colored material we need to color the quads. But only if the texture was not a custom texture
//                if(material.renderInfo.useVertexColoring() && !CustomTextureCreator.exists(baseTexture + "_" + material.identifier)) {
//                    int color = (material.renderInfo).getVertexColor();
//
//                    ImmutableList.Builder<BakedQuad> quads = ImmutableList.builder();
//                    // ItemLayerModel.BakedModel only uses general quads
//                    for(BakedQuad quad : bakedModel2.getQuads(null, null, 0)) {
//                        quads.add(ModelHelper.colorQuad(color, quad));
//                    }
//
//                    // create a new model with the colored quads
//                    bakedModel2 = new BakedSimple(quads.build(), map, bakedModel2);
//                }
//
//                bakedMaterialModel.addMaterialModel(material, bakedModel2);
//            }
//        }

        return bakedMaterialModel;
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return ImmutableList.of();
    }

    @Override
    public Collection<ResourceLocation> getTextures() {
        return textures;
    }

    @Override
    public IModelState getDefaultState() {
        return TRSRTransformation.identity();
    }
}