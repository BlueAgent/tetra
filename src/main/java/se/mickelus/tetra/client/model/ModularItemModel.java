package se.mickelus.tetra.client.model;

import javax.annotation.Nullable;
import javax.vecmath.Vector4f;

import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.ISprite;
import net.minecraftforge.client.model.BakedItemModel;
import net.minecraftforge.client.model.ItemTextureQuadConverter;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.versions.forge.ForgeVersion;

import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.pipeline.IVertexConsumer;
import net.minecraftforge.client.model.pipeline.TRSRTransformer;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;

import java.util.*;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Forge reimplementation of vanilla {@link ItemModelGenerator}, i.e. builtin/generated models,
 * with the following changes:
 * - Represented as a true {@link IUnbakedModel} so it can be baked as usual instead of using
 *   special-case logic like vanilla does.
 * - Various fixes in the baking logic.
 * - Not limited to 4 layers maximum.
 */
public final class ModularItemModel implements IUnbakedModel {
    private static final Direction[] HORIZONTALS = {Direction.UP, Direction.DOWN};
    private static final Direction[] VERTICALS = {Direction.WEST, Direction.EAST};

    private final ImmutableList<ResourceLocation> textures;

    public ModularItemModel(ImmutableList<ResourceLocation> textures) {
        this.textures = textures;
    }

    private static ImmutableList<ResourceLocation> getTextures(BlockModel model)
    {
        ImmutableList.Builder<ResourceLocation> builder = ImmutableList.builder();
        for(int i = 0; model.isTexturePresent("layer" + i); i++)
        {
            builder.add(new ResourceLocation(model.resolveTextureName("layer" + i)));
        }
        return builder.build();
    }

    @Override
    public Collection<ResourceLocation> getTextures(Function<ResourceLocation, IUnbakedModel> modelGetter, Set<String> missingTextureErrors) {
        return textures;
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return Collections.emptyList();
    }

    @Override
    public ModularItemModel retexture(ImmutableMap<String, String> textures)
    {
        ImmutableList.Builder<ResourceLocation> builder = ImmutableList.builder();
        for(int i = 0; i < textures.size() + this.textures.size(); i++)
        {
            if(textures.containsKey("layer" + i))
            {
                builder.add(new ResourceLocation(textures.get("layer" + i)));
            }
            else if(i < this.textures.size())
            {
                builder.add(this.textures.get(i));
            }
        }
        return new ModularItemModel(builder.build());
    }

    @Nullable
    @Override
    public IBakedModel bake(ModelBakery bakery, Function<ResourceLocation, TextureAtlasSprite> spriteGetter, ISprite sprite, VertexFormat format) {
        ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();

        Optional<TRSRTransformation> transform = sprite.getState().apply(Optional.empty());
        ImmutableMap<TransformType, TRSRTransformation> map = PerspectiveMapWrapper.getTransforms(sprite.getState());
        boolean identity = !transform.isPresent() || transform.get().isIdentity();

        for(int i = 0; i < textures.size(); i++) {
            TextureAtlasSprite tas = spriteGetter.apply(textures.get(i));
            builder.addAll(getQuadsForSprite(i, tas, format, transform));
        }
        TextureAtlasSprite particle = spriteGetter.apply(textures.isEmpty() ? new ResourceLocation("missingno") : textures.get(0));

        return new BakedItemModel(builder.build(), particle, map, ItemOverrideList.EMPTY, identity);
    }

    public static List<BakedQuad> getQuadsForSprite(int tint, TextureAtlasSprite sprite, VertexFormat format, Optional<TRSRTransformation> transform) {
        ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();

//        int uMax = sprite.getWidth();
//        int vMax = sprite.getHeight();
//
//        FaceData faceData = new FaceData(uMax, vMax);
//        boolean anyTransparent = false;
//
//        for(int f = 0; f < sprite.getFrameCount(); f++) {
//            boolean transparentWest;
//            boolean[] transparentUp = new boolean[uMax];
//            Arrays.fill(transparentUp, true);
//            for(int v = 0; v < vMax; v++) {
//                transparentWest = true;
//                for(int u = 0; u < uMax; u++) {
//                    int alpha = sprite.getPixelRGBA(f, u, v) >> 24 & 0xFF;
//                    boolean transparent = alpha / 255f <= 0.1f;
//
//                    if (!transparent && alpha < 255) {
//                        anyTransparent = true;
//                    }
//
//                    if(transparentWest && !transparent){ // left - transparent, right - opaque
//                        faceData.set(Direction.WEST, u, v);
//                    }
////                    if(!transparentWest && transparent){ // left - opaque, right - transparent
//                        faceData.set(Direction.EAST, u, v);
////                    }
////                    if(transparentUp[u] && !transparent){ // up - transparent, down - opaque
//                        faceData.set(Direction.UP, u, v);
////                    }
////                    if(!transparentUp[u] && transparent){ // up - opaque, down - transparent
//                        faceData.set(Direction.DOWN, u, v);
////                    }
//
//                    transparentWest = transparent;
//                    transparentUp[u] = transparent;
//                }
//
//                if(!transparentWest){ // last - opaque
//                    faceData.set(Direction.EAST, uMax-1, v);
//                }
//            }
//            // last line
//            for(int u = 0; u < uMax; u++){
//                if(!transparentUp[u]){
//                    faceData.set(Direction.DOWN, u, vMax-1);
//                }
//            }
//        }
//
//        // horizontal quads
//        for (Direction facing : HORIZONTALS) {
//            for (int v = 0; v < vMax; v++) {
//                int uStart = 0, uEnd = uMax;
//                boolean building = false;
//                for (int u = 0; u < uMax; u++) {
//                    boolean face = faceData.get(facing, u, v);
//                    if (!anyTransparent) {
//                        if (face) {
//                            if (!building) {
//                                building = true;
//                                uStart = u;
//                            }
//                            uEnd = u + 1;
//                        }
//                    } else {
//                        if (building && !face) { // finish current quad
//                            // make quad [uStart, u]
//                            int off = facing == Direction.DOWN ? 1 : 0;
//                            builder.add(buildSideQuad(format, transform, facing, tint, sprite, uStart, v + off, u - uStart));
//                            building = false;
//                        } else if (!building && face) { // start new quad
//                            building = true;
//                            uStart = u;
//                        }
//                    }
//                }
//                if (building) { // build remaining quad
//                    // make quad [uStart, uEnd]
//                    int off = facing == Direction.DOWN ? 1 : 0;
//                    builder.add(buildSideQuad(format, transform, facing, tint, sprite, uStart, v+off, uEnd-uStart));
//                }
//            }
//
//        }

        // vertical quads
//        for (Direction facing : VERTICALS) {
//            for (int u = 0; u < uMax; u++) {
//                int vStart = 0, vEnd = vMax;
//                boolean building = false;
//                for (int v = 0; v < vMax; v++) {
//                    boolean face = faceData.get(facing, u, v);
//                    if (!anyTransparent) {
//                        if (face) {
//                            if (!building) {
//                                building = true;
//                                vStart = v;
//                            }
//                            vEnd = v + 1;
//                        }
//                    }
//                    else {
//                        if (building && !face) { // finish current quad
//                            // make quad [vStart, v]
//                            int off = facing == Direction.EAST ? 1 : 0;
//                            builder.add(buildSideQuad(format, transform, facing, tint, sprite, u+off, vStart, v-vStart));
//                            building = false;
//                        }
//                        else if (!building && face) { // start new quad
//                            building = true;
//                            vStart = v;
//                        }
//                    }
//                }
//                if (building) { // build remaining quad
//                    // make quad [vStart, vEnd]
//                    int off = facing == Direction.EAST ? 1 : 0;
//                    builder.add(buildSideQuad(format, transform, facing, tint, sprite, u+off, vStart, vEnd-vStart));
//                }
//            }
//        }

        // front
        builder.add(buildQuad(format, transform, Direction.NORTH, sprite, tint,
                0, 0, 7.5f / 16f, sprite.getMinU(), sprite.getMaxV(),
                0, 1, 7.5f / 16f, sprite.getMinU(), sprite.getMinV(),
                1, 1, 7.5f / 16f, sprite.getMaxU(), sprite.getMinV(),
                1, 0, 7.5f / 16f, sprite.getMaxU(), sprite.getMaxV()
        ));
        // back
        builder.add(buildQuad(format, transform, Direction.SOUTH, sprite, tint,
                0, 0, 8.5f / 16f, sprite.getMinU(), sprite.getMaxV(),
                1, 0, 8.5f / 16f, sprite.getMaxU(), sprite.getMaxV(),
                1, 1, 8.5f / 16f, sprite.getMaxU(), sprite.getMinV(),
                0, 1, 8.5f / 16f, sprite.getMinU(), sprite.getMinV()
        ));

//        builder.addAll(ItemTextureQuadConverter.convertTexture(format, transform.orElse(TRSRTransformation.identity()),
//                sprite, sprite, 0, Direction.EAST, 0xFFFFFFFF, tint));
//
        return builder.build();
    }

    private static class FaceData {
        private final EnumMap<Direction, BitSet> data = new EnumMap<>(Direction.class);

        private final int vMax;

        FaceData(int uMax, int vMax)
        {
            this.vMax = vMax;

            data.put(Direction.WEST, new BitSet(uMax * vMax));
            data.put(Direction.EAST, new BitSet(uMax * vMax));
            data.put(Direction.UP,   new BitSet(uMax * vMax));
            data.put(Direction.DOWN, new BitSet(uMax * vMax));
        }

        public void set(Direction facing, int u, int v)
        {
            data.get(facing).set(getIndex(u, v));
        }

        public boolean get(Direction facing, int u, int v)
        {
            return data.get(facing).get(getIndex(u, v));
        }

        private int getIndex(int u, int v)
        {
            return v * vMax + u;
        }
    }

    private static BakedQuad buildSideQuad(VertexFormat format, Optional<TRSRTransformation> transform, Direction side, int tint, TextureAtlasSprite sprite, int u, int v, int size) {
        final float eps = 1e-2f;

        int width = sprite.getWidth();
        int height = sprite.getHeight();

        float x0 = (float) u / width;
        float y0 = (float) v / height;
        float x1 = x0, y1 = y0;
        float z0 = 7.5f / 16f, z1 = 8.5f / 16f;

        switch(side)
        {
            case WEST:
                z0 = 8.5f / 16f;
                z1 = 7.5f / 16f;
            case EAST:
                y1 = (float) (v + size) / height;
                break;
            case DOWN:
                z0 = 8.5f / 16f;
                z1 = 7.5f / 16f;
            case UP:
                x1 = (float) (u + size) / width;
                break;
            default:
                throw new IllegalArgumentException("can't handle z-oriented side");
        }

        float dx = side.getDirectionVec().getX() * eps / width;
        float dy = side.getDirectionVec().getY() * eps / height;

        float u0 = 16f * (x0 - dx);
        float u1 = 16f * (x1 - dx);
        float v0 = 16f * (1f - y0 - dy);
        float v1 = 16f * (1f - y1 - dy);

        return buildQuad(
                format, transform, remap(side), sprite, tint,
                x0, y0, z0, sprite.getInterpolatedU(u0), sprite.getInterpolatedV(v0),
                x1, y1, z0, sprite.getInterpolatedU(u1), sprite.getInterpolatedV(v1),
                x1, y1, z1, sprite.getInterpolatedU(u1), sprite.getInterpolatedV(v1),
                x0, y0, z1, sprite.getInterpolatedU(u0), sprite.getInterpolatedV(v0)
        );
    }

    private static Direction remap(Direction side) {
        // getOpposite is related to the swapping of V direction
        return side.getAxis() == Direction.Axis.Y ? side.getOpposite() : side;
    }

    private static BakedQuad buildQuad(VertexFormat format, Optional<TRSRTransformation> transform, Direction side,
            TextureAtlasSprite sprite, int tint,
            float x0, float y0, float z0, float u0, float v0,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3) {
        UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(format);

        builder.setQuadTint(tint);
        builder.setQuadOrientation(side);
        builder.setTexture(sprite);

        boolean hasTransform = transform.isPresent() && !transform.get().isIdentity();
        IVertexConsumer consumer = hasTransform ? new TRSRTransformer(builder, transform.get()) : builder;

        putVertex(consumer, format, side, x0, y0, z0, u0, v0, 0xFFFFFFFF);
        putVertex(consumer, format, side, x1, y1, z1, u1, v1, 0xFFFFFFFF);
        putVertex(consumer, format, side, x2, y2, z2, u2, v2, 0xFFFFFFFF);
        putVertex(consumer, format, side, x3, y3, z3, u3, v3, 0xFFFFFFFF);

        return builder.build();
    }

    private static void putVertex(IVertexConsumer consumer, VertexFormat format, Direction side, float x, float y, float z, float u,
            float v, int color) {
        for(int e = 0; e < format.getElementCount(); e++) {
            switch(format.getElement(e).getUsage()) {
                case POSITION:
                    consumer.put(e, x, y, z, 1f);
                    break;
                case COLOR:
                    float r = ((color >> 16) & 0xFF) / 255f; // red
                    float g = ((color >>  8) & 0xFF) / 255f; // green
                    float b = ((color >>  0) & 0xFF) / 255f; // blue
                    float a = ((color >> 24) & 0xFF) / 255f; // alpha
                    consumer.put(e, r, g, b, a);
                    break;
                case NORMAL:
                    float offX = (float) side.getXOffset();
                    float offY = (float) side.getYOffset();
                    float offZ = (float) side.getZOffset();
                    consumer.put(e, offX, offY, offZ, 0f);
                    break;
                case UV:
                    if(format.getElement(e).getIndex() == 0) {
                        consumer.put(e, u, v, 0f, 1f);
                        break;
                    }
                    // else fallthrough to default
                default:
                    consumer.put(e);
                    break;
            }
        }
    }
}