package cn.zbx1425.mtrsteamloco.render.train;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class SteamSmokeParticle extends SingleQuadParticle {

    SteamSmokeParticle(ClientLevel clientLevel, double x, double y, double z, double xAux, double yAux, double zAux, SpriteSet sprite) {
        super(clientLevel, x, y, z, xAux, yAux, zAux, sprite.first());
        this.scale(3.0f);
        this.setSize(0.25f, 0.25f);
        this.lifetime = this.random.nextInt(10) + 50;
        this.gravity = 3.0E-6f;
        this.hasPhysics = true;
        this.xd = xAux;
        this.yd = yAux + (double)(this.random.nextFloat() / 500.0f);
        this.zd = zAux;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime || this.alpha <= 0.0f) {
            this.remove();
            return;
        }
        this.xd += (double)(this.random.nextFloat() / 50.0f * (float)(this.random.nextBoolean() ? 1 : -1));
        this.zd += (double)(this.random.nextFloat() / 50.0f * (float)(this.random.nextBoolean() ? 1 : -1));
        this.yd -= (double)this.gravity;
        this.move(this.xd, this.yd, this.zd);

        if (this.age < this.lifetime * 0.4) {
            this.quadSize += 0.2;
            this.alpha = 1;
        } else {
            this.quadSize += 0.1;
            this.alpha = 1 - (this.age - this.lifetime * 0.4f) / (this.lifetime * 0.6f);
        }
        if (this.y == this.yo) {
            this.remove();
        }
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Provider(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType particleOptions, ClientLevel clientLevel, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            SteamSmokeParticle campfireSmokeParticle = new SteamSmokeParticle(clientLevel, x, y, z, xAux, yAux, zAux, this.sprites);
            campfireSmokeParticle.setAlpha(1f);
            return campfireSmokeParticle;
        }
    }
}
