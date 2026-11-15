package net.ringsofash.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Volute de fumee verte lumineuse (couleur des cristaux de Morgul) qui monte de l'ichor et s'etale en s'effacant. */
public class IchorSmokeParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected IchorSmokeParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = xd; this.yd = yd; this.zd = zd;
        this.lifetime = 50 + this.random.nextInt(50);
        this.quadSize = 0.9F + this.random.nextFloat() * 0.9F;
        this.friction = 0.97F;
        this.gravity = 0F;
        this.hasPhysics = false;
        float k = this.random.nextFloat();
        this.setColor(0.35F + 0.35F * k, 0.92F + 0.08F * k, 0.50F + 0.35F * k);       // du vert des cristaux au vert pale
        this.alpha = 0.75F;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
        this.quadSize *= 1.012F;
        this.alpha = 0.75F * (1F - (float) this.age / this.lifetime);
    }

    @Override
    public int getLightColor(float partialTick) { return 0xF000F0; }                 // lumineuse, meme dans le noir

    @Override
    public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new IchorSmokeParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
